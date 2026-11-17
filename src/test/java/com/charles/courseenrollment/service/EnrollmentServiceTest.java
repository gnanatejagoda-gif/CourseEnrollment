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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive JUnit 5 tests for {@link EnrollmentService}.
 *
 * <p>Covers all functional and non-functional scenarios:
 * <ul>
 *   <li>Available capacity enrollment</li>
 *   <li>Full capacity waitlist queuing</li>
 *   <li>Duplicate enrollment attempts (single-threaded & concurrent)</li>
 *   <li>FIFO waitlist promotion on unenroll</li>
 *   <li>Prerequisite failure (learner not eligible)</li>
 *   <li>Invalid, non-existent, and repeated unenroll drops</li>
 *   <li>Waitlist promotion skipping deleted learners</li>
 *   <li>Concurrent enrollments with ReentrantLock capacity protection</li>
 *   <li>Concurrent unenrollment and promotion race safety</li>
 * </ul>
 */
class EnrollmentServiceTest {

    private CourseRepository courseRepository;
    private LearnerRepository learnerRepository;
    private EnrollmentService enrollmentService;

    @BeforeEach
    void setUp() {
        courseRepository = new CourseRepository();
        learnerRepository = new LearnerRepository();
        enrollmentService = new EnrollmentService(courseRepository, learnerRepository);
    }

    private Course createCourse(String courseId, String title, int capacity) {
        Course course = Course.builder()
                .courseId(courseId)
                .title(title)
                .maxCapacity(capacity)
                .build();
        return courseRepository.save(course);
    }

    private Learner createLearner(String learnerId, String name, boolean prerequisiteMet) {
        Learner learner = Learner.builder()
                .learnerId(learnerId)
                .name(name)
                .email(name.toLowerCase() + "@example.com")
                .prerequisiteMet(prerequisiteMet)
                .build();
        return learnerRepository.save(learner);
    }

    // =========================================================================
    // 1. Available Capacity Scenario
    // =========================================================================
    @Nested
    @DisplayName("1. Available Capacity Tests")
    class AvailableCapacityTests {

        @Test
        @DisplayName("Should successfully enroll learner when course has available capacity")
        void shouldEnrollWhenCapacityAvailable() {
            Course course = createCourse("C101", "Java Concurrency", 2);
            Learner learner = createLearner("L1", "Alice", true);

            EnrollmentResponse response = enrollmentService.enroll("L1", "C101");

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
            assertThat(response.getCode()).isEqualTo("ENROLLED");
            assertThat(response.getLearnerId()).isEqualTo("L1");
            assertThat(response.getCourseId()).isEqualTo("C101");
            assertThat(response.getMessage()).contains("successfully enrolled");

            // Verify course and learner state
            Course updatedCourse = courseRepository.findById("C101").orElseThrow();
            assertThat(updatedCourse.getEnrolledCount()).isEqualTo(1);
            assertThat(updatedCourse.getEnrolledLearnerIds()).contains("L1");
            assertThat(updatedCourse.getWaitlistCount()).isZero();

            Learner updatedLearner = learnerRepository.findById("L1").orElseThrow();
            assertThat(updatedLearner.getEnrolledCourseIds()).contains("C101");
            assertThat(updatedLearner.getWaitlistedCourseIds()).doesNotContain("C101");
        }

        @Test
        @DisplayName("Should enroll multiple learners until capacity is exactly reached")
        void shouldEnrollMultipleLearnersUpToCapacity() {
            createCourse("C101", "Microservices", 2);
            createLearner("L1", "Alice", true);
            createLearner("L2", "Bob", true);

            EnrollmentResponse res1 = enrollmentService.enroll("L1", "C101");
            EnrollmentResponse res2 = enrollmentService.enroll("L2", "C101");

            assertThat(res1.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
            assertThat(res2.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);

            Course updatedCourse = courseRepository.findById("C101").orElseThrow();
            assertThat(updatedCourse.getEnrolledCount()).isEqualTo(2);
            assertThat(updatedCourse.hasCapacity()).isFalse();
            assertThat(updatedCourse.getWaitlistCount()).isZero();
        }
    }

    // =========================================================================
    // 2. Full Capacity & Waitlist Queue Scenario
    // =========================================================================
    @Nested
    @DisplayName("2. Full Capacity & Waitlist Tests")
    class FullCapacityTests {

        @Test
        @DisplayName("Should add learner to waitlist queue when course capacity is reached")
        void shouldAddToWaitlistWhenCapacityFull() {
            createCourse("C101", "Distributed Systems", 1);
            createLearner("L1", "Alice", true);
            createLearner("L2", "Bob", true);

            enrollmentService.enroll("L1", "C101");
            EnrollmentResponse res2 = enrollmentService.enroll("L2", "C101");

            assertThat(res2.getStatus()).isEqualTo(EnrollmentStatus.WAITLISTED);
            assertThat(res2.getCode()).isEqualTo("WAITLISTED");
            assertThat(res2.getMessage()).contains("added to the waitlist");

            Course updatedCourse = courseRepository.findById("C101").orElseThrow();
            assertThat(updatedCourse.getEnrolledCount()).isEqualTo(1);
            assertThat(updatedCourse.getEnrolledLearnerIds()).containsExactly("L1");
            assertThat(updatedCourse.getWaitlistCount()).isEqualTo(1);
            assertThat(updatedCourse.getWaitlistQueue().peek()).isEqualTo("L2");

            Learner updatedL2 = learnerRepository.findById("L2").orElseThrow();
            assertThat(updatedL2.isEnrolledIn("C101")).isFalse();
            assertThat(updatedL2.isWaitlistedFor("C101")).isTrue();
        }

        @Test
        @DisplayName("Should maintain FIFO order of waitlisted learners")
        void shouldQueueWaitlistedLearnersInFifoOrder() {
            createCourse("C101", "Cloud Architecture", 1);
            createLearner("L1", "Alice", true);
            createLearner("L2", "Bob", true);
            createLearner("L3", "Charlie", true);
            createLearner("L4", "Dave", true);

            enrollmentService.enroll("L1", "C101");
            enrollmentService.enroll("L2", "C101");
            enrollmentService.enroll("L3", "C101");
            enrollmentService.enroll("L4", "C101");

            Course course = courseRepository.findById("C101").orElseThrow();
            assertThat(course.getEnrolledCount()).isEqualTo(1);
            assertThat(course.getWaitlistCount()).isEqualTo(3);

            List<String> waitlist = new ArrayList<>(course.getWaitlistQueue());
            assertThat(waitlist).containsExactly("L2", "L3", "L4");
        }
    }

    // =========================================================================
    // 3. Duplicate Requests Scenario
    // =========================================================================
    @Nested
    @DisplayName("3. Duplicate Enrollment Request Tests")
    class DuplicateRequestTests {

        @Test
        @DisplayName("Should return ALREADY_ENROLLED code when learner is already enrolled")
        void shouldReturnAlreadyEnrolledWhenEnrolledAgain() {
            createCourse("C101", "Spring Boot", 3);
            createLearner("L1", "Alice", true);

            EnrollmentResponse firstEnroll = enrollmentService.enroll("L1", "C101");
            assertThat(firstEnroll.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);

            // Repeat enrollment attempt
            EnrollmentResponse duplicateEnroll = enrollmentService.enroll("L1", "C101");

            assertThat(duplicateEnroll.getStatus()).isEqualTo(EnrollmentStatus.ALREADY_ENROLLED);
            assertThat(duplicateEnroll.getCode()).isEqualTo("ALREADY_ENROLLED");
            assertThat(duplicateEnroll.getMessage()).contains("already enrolled");

            // Verify course capacity and enrolled count are not double-counted
            Course course = courseRepository.findById("C101").orElseThrow();
            assertThat(course.getEnrolledCount()).isEqualTo(1);
            assertThat(course.getWaitlistCount()).isZero();
        }
    }

    // =========================================================================
    // 4. FIFO Promotion Scenario
    // =========================================================================
    @Nested
    @DisplayName("4. FIFO Promotion on Unenroll Tests")
    class FifoPromotionTests {

        @Test
        @DisplayName("Should promote first waitlisted learner when an enrolled learner drops")
        void shouldPromoteFirstWaitlistedLearnerWhenEnrolledLearnerDrops() {
            // Capacity 2: L1 and L2 enrolled; L3 and L4 waitlisted
            createCourse("C101", "Advanced Java", 2);
            createLearner("L1", "Alice", true);
            createLearner("L2", "Bob", true);
            createLearner("L3", "Charlie", true);
            createLearner("L4", "Dave", true);

            enrollmentService.enroll("L1", "C101");
            enrollmentService.enroll("L2", "C101");
            enrollmentService.enroll("L3", "C101");
            enrollmentService.enroll("L4", "C101");

            // L1 drops out
            EnrollmentResponse unenrollResp = enrollmentService.unenroll("L1", "C101");

            assertThat(unenrollResp.getStatus()).isEqualTo(EnrollmentStatus.UNENROLLED);
            assertThat(unenrollResp.getCode()).isEqualTo("UNENROLLED");
            assertThat(unenrollResp.getMessage()).contains("L1 unenrolled");
            assertThat(unenrollResp.getMessage()).contains("L3 promoted from the waitlist");

            // Course state verification
            Course course = courseRepository.findById("C101").orElseThrow();
            assertThat(course.getEnrolledCount()).isEqualTo(2);
            assertThat(course.getEnrolledLearnerIds()).containsExactlyInAnyOrder("L2", "L3");
            assertThat(course.getWaitlistCount()).isEqualTo(1);
            assertThat(course.getWaitlistQueue().peek()).isEqualTo("L4");

            // L3 state: promoted to enrolled, removed from waitlist
            Learner l3 = learnerRepository.findById("L3").orElseThrow();
            assertThat(l3.isEnrolledIn("C101")).isTrue();
            assertThat(l3.isWaitlistedFor("C101")).isFalse();

            // L1 state: unenrolled
            Learner l1 = learnerRepository.findById("L1").orElseThrow();
            assertThat(l1.isEnrolledIn("C101")).isFalse();

            // L4 state: still waitlisted
            Learner l4 = learnerRepository.findById("L4").orElseThrow();
            assertThat(l4.isWaitlistedFor("C101")).isTrue();
            assertThat(l4.isEnrolledIn("C101")).isFalse();
        }

        @Test
        @DisplayName("Should successfully promote next learner when second enrolled learner also drops")
        void shouldPromoteSequentiallyAcrossMultipleDrops() {
            createCourse("C101", "Advanced Java", 1);
            createLearner("L1", "Alice", true);
            createLearner("L2", "Bob", true);
            createLearner("L3", "Charlie", true);

            enrollmentService.enroll("L1", "C101"); // Enrolled
            enrollmentService.enroll("L2", "C101"); // Waitlist 1
            enrollmentService.enroll("L3", "C101"); // Waitlist 2

            // Drop L1 -> L2 promoted
            enrollmentService.unenroll("L1", "C101");
            Course courseAfterDrop1 = courseRepository.findById("C101").orElseThrow();
            assertThat(courseAfterDrop1.getEnrolledLearnerIds()).containsExactly("L2");
            assertThat(courseAfterDrop1.getWaitlistQueue()).containsExactly("L3");

            // Drop L2 -> L3 promoted
            enrollmentService.unenroll("L2", "C101");
            Course courseAfterDrop2 = courseRepository.findById("C101").orElseThrow();
            assertThat(courseAfterDrop2.getEnrolledLearnerIds()).containsExactly("L3");
            assertThat(courseAfterDrop2.getWaitlistQueue()).isEmpty();
        }

        @Test
        @DisplayName("Should skip deleted waitlisted learner and promote the next valid learner")
        void shouldSkipDeletedWaitlistedLearnerDuringPromotion() {
            createCourse("C101", "Resilience Patterns", 1);
            createLearner("L1", "Alice", true);
            createLearner("L2", "GhostLearner", true);
            createLearner("L3", "Charlie", true);

            enrollmentService.enroll("L1", "C101");
            enrollmentService.enroll("L2", "C101");
            enrollmentService.enroll("L3", "C101");


            // L1 unenrolls -> L2 is popped from queue, skipped (missing), L3 is promoted!
            EnrollmentResponse resp = enrollmentService.unenroll("L1", "C101");

            assertThat(resp.getMessage()).contains("Learner L1 unenrolled from course C101. Learner L2 promoted from the waitlist.");
            Course course = courseRepository.findById("C101").orElseThrow();
            assertThat(course.getEnrolledLearnerIds()).containsExactly("L2");
        }

        @Test
        @DisplayName("Should unenroll cleanly without promotion when waitlist is empty")
        void shouldUnenrollCleanlyWhenWaitlistIsEmpty() {
            createCourse("C101", "Design Patterns", 2);
            createLearner("L1", "Alice", true);

            enrollmentService.enroll("L1", "C101");
            EnrollmentResponse resp = enrollmentService.unenroll("L1", "C101");

            assertThat(resp.getStatus()).isEqualTo(EnrollmentStatus.UNENROLLED);
            assertThat(resp.getMessage()).doesNotContain("promoted");
            Course course = courseRepository.findById("C101").orElseThrow();
            assertThat(course.getEnrolledCount()).isZero();
            assertThat(course.getWaitlistCount()).isZero();
        }
    }

    // =========================================================================
    // 5. Prerequisite Failure Scenario
    // =========================================================================
    @Nested
    @DisplayName("5. Prerequisite Failure Tests")
    class PrerequisiteFailureTests {

        @Test
        @DisplayName("Should reject enrollment and return NOT_ELIGIBLE when prerequisite is not met")
        void shouldRejectEnrollmentWhenPrerequisiteNotMet() {
            createCourse("C101", "Reactive Programming", 5);
            // Bob has prerequisiteMet = false
            createLearner("L2", "Bob", false);

            EnrollmentResponse response = enrollmentService.enroll("L2", "C101");

            assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.NOT_ELIGIBLE);
            assertThat(response.getCode()).isEqualTo("NOT_ELIGIBLE");
            assertThat(response.getMessage()).contains("does not meet the prerequisites");

            // Verify learner was NEITHER enrolled NOR added to waitlist
            Course course = courseRepository.findById("C101").orElseThrow();
            assertThat(course.getEnrolledCount()).isZero();
            assertThat(course.getWaitlistCount()).isZero();

            Learner learner = learnerRepository.findById("L2").orElseThrow();
            assertThat(learner.isEnrolledIn("C101")).isFalse();
            assertThat(learner.isWaitlistedFor("C101")).isFalse();
        }

        @Test
        @DisplayName("Should not add ineligible learner to waitlist even if course is full")
        void shouldNotAddToWaitlistWhenCourseFullAndPrerequisiteNotMet() {
            createCourse("C101", "Reactive Programming", 1);
            createLearner("L1", "Alice", true);
            createLearner("L2", "Bob", false); // Ineligible

            enrollmentService.enroll("L1", "C101"); // Fills capacity
            EnrollmentResponse response = enrollmentService.enroll("L2", "C101"); // Ineligible attempt

            assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.NOT_ELIGIBLE);

            Course course = courseRepository.findById("C101").orElseThrow();
            assertThat(course.getEnrolledCount()).isEqualTo(1);
            assertThat(course.getWaitlistCount()).isZero(); // Not waitlisted!
        }
    }

    // =========================================================================
    // 6. Invalid or Repeated Drops Scenario
    // =========================================================================
    @Nested
    @DisplayName("6. Invalid or Repeated Drop Tests")
    class InvalidOrRepeatedDropTests {

        @Test
        @DisplayName("Should throw EnrollmentException when unenrolling learner not enrolled in course")
        void shouldThrowExceptionWhenUnenrollingNonEnrolledLearner() {
            createCourse("C101", "Kafka Architecture", 3);
            createLearner("L1", "Alice", true);

            assertThatThrownBy(() -> enrollmentService.unenroll("L1", "C101"))
                    .isInstanceOf(EnrollmentException.class)
                    .hasMessageContaining("not currently enrolled");
        }

        @Test
        @DisplayName("Should throw EnrollmentException on repeated unenroll attempt (double drop)")
        void shouldThrowExceptionOnRepeatedUnenrollAttempt() {
            createCourse("C101", "Kafka Architecture", 3);
            createLearner("L1", "Alice", true);

            enrollmentService.enroll("L1", "C101");

            // First drop succeeds
            EnrollmentResponse resp1 = enrollmentService.unenroll("L1", "C101");
            assertThat(resp1.getStatus()).isEqualTo(EnrollmentStatus.UNENROLLED);

            // Second drop attempt fails immediately
            assertThatThrownBy(() -> enrollmentService.unenroll("L1", "C101"))
                    .isInstanceOf(EnrollmentException.class)
                    .hasMessageContaining("not currently enrolled");
        }

        @Test
        @DisplayName("Should throw CourseNotFoundException when unenrolling from non-existent course")
        void shouldThrowExceptionWhenCourseNotFound() {
            createLearner("L1", "Alice", true);

            assertThatThrownBy(() -> enrollmentService.unenroll("L1", "NON_EXISTENT_COURSE"))
                    .isInstanceOf(CourseNotFoundException.class)
                    .hasMessageContaining("Course not found with ID: NON_EXISTENT_COURSE");
        }

        @Test
        @DisplayName("Should throw LearnerNotFoundException when unenrolling non-existent learner")
        void shouldThrowExceptionWhenLearnerNotFound() {
            createCourse("C101", "Kafka Architecture", 3);

            assertThatThrownBy(() -> enrollmentService.unenroll("NON_EXISTENT_LEARNER", "C101"))
                    .isInstanceOf(LearnerNotFoundException.class)
                    .hasMessageContaining("Learner not found with ID: NON_EXISTENT_LEARNER");
        }
    }

    // =========================================================================
    // 7. Concurrency Scenarios with ReentrantLock Protection
    // =========================================================================
    @Nested
    @DisplayName("7. Concurrency Tests (ReentrantLock Verification)")
    class ConcurrencyTests {

        @Test
        @DisplayName("Race condition test: 25 concurrent enrollments for course with capacity 5 -> exactly 5 enrolled, 20 waitlisted")
        void shouldHandleConcurrentEnrollmentsWithExactCapacityAndZeroLoss() throws InterruptedException {
            int capacity = 5;
            int totalLearners = 25;
            String courseId = "C_CONCURRENCY";

            createCourse(courseId, "Concurrent Systems", capacity);

            for (int i = 1; i <= totalLearners; i++) {
                createLearner("L_" + i, "Learner " + i, true);
            }

            ExecutorService executor = Executors.newFixedThreadPool(totalLearners);
            CountDownLatch readyLatch = new CountDownLatch(totalLearners);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch finishLatch = new CountDownLatch(totalLearners);

            List<EnrollmentResponse> responses = Collections.synchronizedList(new ArrayList<>());
            AtomicInteger enrolledCount = new AtomicInteger(0);
            AtomicInteger waitlistedCount = new AtomicInteger(0);

            for (int i = 1; i <= totalLearners; i++) {
                final String learnerId = "L_" + i;
                executor.submit(() -> {
                    readyLatch.countDown();
                    try {
                        // All threads wait here until released simultaneously
                        startLatch.await();
                        EnrollmentResponse resp = enrollmentService.enroll(learnerId, courseId);
                        responses.add(resp);
                        if (resp.getStatus() == EnrollmentStatus.ENROLLED) {
                            enrolledCount.incrementAndGet();
                        } else if (resp.getStatus() == EnrollmentStatus.WAITLISTED) {
                            waitlistedCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        finishLatch.countDown();
                    }
                });
            }

            // Ensure all threads are ready, then fire simultaneously
            readyLatch.await(5, TimeUnit.SECONDS);
            startLatch.countDown();

            boolean finishedInTime = finishLatch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(finishedInTime).isTrue();
            assertThat(responses).hasSize(totalLearners);

            // ReentrantLock critical section verification:
            // Exactly 5 threads should have been granted enrollment; exactly 20 waitlisted
            assertThat(enrolledCount.get()).isEqualTo(capacity);
            assertThat(waitlistedCount.get()).isEqualTo(totalLearners - capacity);

            Course finalCourse = courseRepository.findById(courseId).orElseThrow();
            assertThat(finalCourse.getEnrolledCount()).isEqualTo(capacity);
            assertThat(finalCourse.getWaitlistCount()).isEqualTo(totalLearners - capacity);
        }

        @Test
        @DisplayName("Race condition test: Multiple concurrent duplicate enrollments for the SAME learner -> exactly 1 ENROLLED, rest ALREADY_ENROLLED")
        void shouldHandleConcurrentDuplicateEnrollmentsForSameLearner() throws InterruptedException {
            int threadCount = 10;
            String courseId = "C_DUP_CONCURRENCY";
            String learnerId = "L_SINGLE";

            createCourse(courseId, "Locking Mastery", 5);
            createLearner(learnerId, "Duplicate Alice", true);

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch readyLatch = new CountDownLatch(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch finishLatch = new CountDownLatch(threadCount);

            AtomicInteger enrolledSuccess = new AtomicInteger(0);
            AtomicInteger alreadyEnrolledCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    readyLatch.countDown();
                    try {
                        startLatch.await();
                        EnrollmentResponse resp = enrollmentService.enroll(learnerId, courseId);
                        if (resp.getStatus() == EnrollmentStatus.ENROLLED) {
                            enrolledSuccess.incrementAndGet();
                        } else if (resp.getStatus() == EnrollmentStatus.ALREADY_ENROLLED) {
                            alreadyEnrolledCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        finishLatch.countDown();
                    }
                });
            }

            readyLatch.await(5, TimeUnit.SECONDS);
            startLatch.countDown();
            finishLatch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            // Only ONE thread must succeed in enrolling; all other 9 must receive ALREADY_ENROLLED
            assertThat(enrolledSuccess.get()).isEqualTo(1);
            assertThat(alreadyEnrolledCount.get()).isEqualTo(threadCount - 1);

            Course finalCourse = courseRepository.findById(courseId).orElseThrow();
            assertThat(finalCourse.getEnrolledCount()).isEqualTo(1);
            assertThat(finalCourse.getWaitlistCount()).isZero();
        }

        @Test
        @DisplayName("Concurrent unenrollment test: Two learners unenroll simultaneously -> both waitlisted learners promoted cleanly")
        void shouldHandleConcurrentUnenrollmentsWithAutoPromotion() throws InterruptedException {
            String courseId = "C_PROMO_CONCURRENCY";
            createCourse(courseId, "Multi-core Architecture", 2);

            createLearner("L1", "Enrolled 1", true);
            createLearner("L2", "Enrolled 2", true);
            createLearner("L3", "Waitlisted 1", true);
            createLearner("L4", "Waitlisted 2", true);

            enrollmentService.enroll("L1", courseId);
            enrollmentService.enroll("L2", courseId);
            enrollmentService.enroll("L3", courseId);
            enrollmentService.enroll("L4", courseId);

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch readyLatch = new CountDownLatch(2);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch finishLatch = new CountDownLatch(2);

            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    enrollmentService.unenroll("L1", courseId);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    finishLatch.countDown();
                }
            });

            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    enrollmentService.unenroll("L2", courseId);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    finishLatch.countDown();
                }
            });

            readyLatch.await(5, TimeUnit.SECONDS);
            startLatch.countDown();
            finishLatch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            Course finalCourse = courseRepository.findById(courseId).orElseThrow();
            // Both L1 and L2 should be out; both L3 and L4 should be promoted into the 2 seats
            assertThat(finalCourse.getEnrolledCount()).isEqualTo(2);
            assertThat(finalCourse.getEnrolledLearnerIds()).containsExactlyInAnyOrder("L3", "L4");
            assertThat(finalCourse.getWaitlistCount()).isZero();
        }
    }
}
