package com.charles.courseenrollment.service;

import com.charles.courseenrollment.exception.CourseNotFoundException;
import com.charles.courseenrollment.exception.EnrollmentException;
import com.charles.courseenrollment.exception.LearnerNotFoundException;
import com.charles.courseenrollment.model.Course;
import com.charles.courseenrollment.model.EnrollmentResponse;
import com.charles.courseenrollment.model.EnrollmentStatus;
import com.charles.courseenrollment.model.Learner;
import com.charles.courseenrollment.repository.CourseRepository;
import com.charles.courseenrollment.repository.LearnerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Core business logic for course enrollment and unenrollment.
 *
 * <h3>Concurrency strategy</h3>
 * <p>One fair {@link ReentrantLock} per courseId is maintained in {@code courseLocks}.
 * Fair mode ({@code true}) ensures threads are granted the lock in FIFO order,
 * preventing thread starvation under high concurrency.</p>
 *
 * <p>The lock is acquired only after all pre-lock checks (entity existence,
 * duplicate enroll, prerequisite) have passed, keeping the critical section
 * as narrow as possible.</p>
 *
 * <p><b>Note on courseLocks lifecycle:</b> The {@code courseLocks} map is never pruned.
 * For a production system with a large or dynamic course catalog, replace this with a
 * bounded cache (e.g., Caffeine with time-based expiry) keyed by courseId.</p>
 */
@Service
public class EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);

    private final CourseRepository courseRepository;
    private final LearnerRepository learnerRepository;

    /**
     * Per-course fair locks stored in a {@link ConcurrentHashMap}.
     *
     * <p><b>Memory note:</b> This map grows with every unique courseId encountered
     * and is never evicted. Acceptable for a small in-memory store; replace with
     * a bounded cache (Caffeine, Guava) for a production-scale catalog.</p>
     */
    private final ConcurrentHashMap<String, ReentrantLock> courseLocks = new ConcurrentHashMap<>();

    public EnrollmentService(CourseRepository courseRepository,
                             LearnerRepository learnerRepository) {
        this.courseRepository = courseRepository;
        this.learnerRepository = learnerRepository;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Enroll a learner in a course.
     *
     * <ol>
     *   <li>Validate that both the course and learner exist.</li>
     *   <li>Optimistic duplicate-enroll guard (unsynchronised fast-path — may be stale).
     *       Correctness is guaranteed by the locked double-check in step 4.</li>
     *   <li>Check prerequisite flag → return {@code NOT_ELIGIBLE} if false.</li>
     *   <li>Acquire the per-course fair lock.</li>
     *   <li>Definitive duplicate-enroll guard inside lock (TOCTOU protection).</li>
     *   <li>If capacity available → enroll; otherwise → add to waitlist.</li>
     *   <li>Release lock.</li>
     * </ol>
     *
     * @param learnerId the learner to enroll
     * @param courseId  the target course
     * @return an {@link EnrollmentResponse} describing the outcome
     */
    public EnrollmentResponse enroll(String learnerId, String courseId) {
        // --- 1. Resolve entities (throws 404 if not found) ---
        Course course = fetchCourse(courseId);
        Learner learner = fetchLearner(learnerId);

        // --- 2. Optimistic duplicate-enroll guard ---
        // NOTE: This check is UNSYNCHRONISED and may be stale. It is a performance
        // optimisation (fast-path) only. Correctness is guaranteed by the definitive
        // double-check inside the lock at step 5. Do not rely on this result alone.
        if (learner.isEnrolledIn(courseId)) {
            return EnrollmentResponse.of(learnerId, courseId, EnrollmentStatus.ALREADY_ENROLLED,
                    "Learner " + learnerId + " is already enrolled in course " + courseId + ".");
        }

        // --- 3. Prerequisite check ---
        if (!learner.isPrerequisiteMet()) {
            return EnrollmentResponse.of(learnerId, courseId, EnrollmentStatus.NOT_ELIGIBLE,
                    "Learner " + learnerId + " does not meet the prerequisites for course " + courseId + ".");
        }

        // --- 4. Acquire per-course fair lock ---
        ReentrantLock lock = getLockForCourse(courseId);
        lock.lock();
        try {
            // Re-fetch inside lock to get the latest state
            course = fetchCourse(courseId);
            learner = fetchLearner(learnerId);

            // --- 5. Definitive duplicate-enroll guard inside lock (TOCTOU protection) ---
            if (learner.isEnrolledIn(courseId)) {
                return EnrollmentResponse.of(learnerId, courseId, EnrollmentStatus.ALREADY_ENROLLED,
                        "Learner " + learnerId + " is already enrolled in course " + courseId + ".");
            }

            // --- 6. Enroll or waitlist ---
            if (course.hasCapacity()) {
                course.getEnrolledLearnerIds().add(learnerId);
                learner.getEnrolledCourseIds().add(courseId);
                courseRepository.save(course);
                learnerRepository.save(learner);
                return EnrollmentResponse.of(learnerId, courseId, EnrollmentStatus.ENROLLED,
                        "Learner " + learnerId + " successfully enrolled in course " + courseId + ".");
            } else {
                course.getWaitlistQueue().offer(learnerId);
                learner.getWaitlistedCourseIds().add(courseId);
                courseRepository.save(course);
                learnerRepository.save(learner);
                return EnrollmentResponse.of(learnerId, courseId, EnrollmentStatus.WAITLISTED,
                        "Course " + courseId + " is at full capacity. Learner " + learnerId
                                + " has been added to the waitlist (position "
                                + course.getWaitlistCount() + ").");
            }
        } finally {
            // --- 7. Always release ---
            lock.unlock();
        }
    }

    /**
     * Unenroll a learner from a course, automatically promoting the next
     * valid waitlisted learner (if any) into the freed seat.
     *
     * <p>If the next learner in the waitlist queue no longer exists in the
     * repository, they are skipped and the next candidate is tried. This
     * prevents silent seat-loss when a waitlisted learner was deleted.</p>
     *
     * @param learnerId the learner to unenroll
     * @param courseId  the target course
     * @return an {@link EnrollmentResponse} describing the outcome
     */
    public EnrollmentResponse unenroll(String learnerId, String courseId) {
        // --- 1. Resolve entities (existence check only before lock) ---
        // Result is intentionally discarded here; re-fetched under lock below.
        courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        Learner learner = fetchLearner(learnerId);

        // --- 2. Must be enrolled to unenroll (pre-lock fast-path check) ---
        if (!learner.isEnrolledIn(courseId)) {
            throw new EnrollmentException(
                    "Learner " + learnerId + " is not currently enrolled in course " + courseId + ".");
        }

        // --- 3. Acquire per-course fair lock ---
        ReentrantLock lock = getLockForCourse(courseId);
        lock.lock();
        try {
            Course course = fetchCourse(courseId);
            learner = fetchLearner(learnerId);

            // Re-verify enrollment inside lock (TOCTOU protection)
            if (!learner.isEnrolledIn(courseId)) {
                throw new EnrollmentException(
                        "Learner " + learnerId + " is not currently enrolled in course " + courseId + ".");
            }

            // --- 4. Remove unenrolling learner ---
            course.getEnrolledLearnerIds().remove(learnerId);
            learner.getEnrolledCourseIds().remove(courseId);

            // --- 5. Promote next VALID learner from waitlist ---
            // Loop until we find a learner that still exists in the repository,
            // skipping any that were deleted. This prevents silent seat-loss.
            String promotedLearnerId = null;
            String candidateId;
            while ((candidateId = course.getWaitlistQueue().poll()) != null) {
                Optional<Learner> promotedOpt = learnerRepository.findById(candidateId);
                if (promotedOpt.isPresent()) {
                    Learner promotedLearner = promotedOpt.get();
                    course.getEnrolledLearnerIds().add(candidateId);
                    promotedLearner.getWaitlistedCourseIds().remove(courseId);
                    promotedLearner.getEnrolledCourseIds().add(courseId);
                    learnerRepository.save(promotedLearner);
                    promotedLearnerId = candidateId;
                    break;
                } else {
                    // Waitlisted learner was deleted — skip and try next
                    log.warn("Waitlisted learner [{}] for course [{}] not found in repository; "
                            + "skipping promotion.", candidateId, courseId);
                }
            }

            // --- 6. Single consolidated save after all state mutations ---
            courseRepository.save(course);
            learnerRepository.save(learner);

            if (promotedLearnerId != null) {
                return EnrollmentResponse.of(learnerId, courseId, EnrollmentStatus.UNENROLLED,
                        "Learner " + learnerId + " unenrolled from course " + courseId
                                + ". Learner " + promotedLearnerId + " promoted from the waitlist.");
            }
            return EnrollmentResponse.of(learnerId, courseId, EnrollmentStatus.UNENROLLED,
                    "Learner " + learnerId + " successfully unenrolled from course " + courseId + ".");

        } finally {
            lock.unlock();
        }
    }

    /**
     * Retrieve all courses.
     */
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    /**
     * Retrieve a single course by ID.
     */
    public Course getCourse(String courseId) {
        return fetchCourse(courseId);
    }

    /**
     * Retrieve all learners.
     */
    public List<Learner> getAllLearners() {
        return learnerRepository.findAll();
    }

    /**
     * Retrieve a single learner by ID.
     */
    public Learner getLearner(String learnerId) {
        return fetchLearner(learnerId);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private Course fetchCourse(String courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
    }

    private Learner fetchLearner(String learnerId) {
        return learnerRepository.findById(learnerId)
                .orElseThrow(() -> new LearnerNotFoundException(learnerId));
    }

    /**
     * Returns the fair {@link ReentrantLock} for the given courseId,
     * creating one atomically via {@link ConcurrentHashMap#computeIfAbsent} if absent.
     */
    private ReentrantLock getLockForCourse(String courseId) {
        return courseLocks.computeIfAbsent(courseId, id -> new ReentrantLock(true));
    }
}
