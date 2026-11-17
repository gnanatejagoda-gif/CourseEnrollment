package com.charles.courseenrollment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Course domain entity stored in-memory.
 *
 * <p>Collection fields use thread-safe implementations to prevent
 * {@link java.util.ConcurrentModificationException} when {@code findAll()}
 * iterates enrolled IDs concurrently with enrollment mutations:</p>
 * <ul>
 *   <li>{@code enrolledLearnerIds} — {@link ConcurrentHashMap}-backed {@link Set}
 *       for thread-safe add/remove/contains and concurrent iteration.</li>
 *   <li>{@code waitlistQueue} — {@link LinkedList} accessed only inside the
 *       per-course {@link java.util.concurrent.locks.ReentrantLock}, so plain
 *       LinkedList is safe here.</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    private String courseId;
    private String title;
    private String description;
    private int maxCapacity;

    /**
     * Set of currently enrolled learner IDs.
     * Backed by {@link ConcurrentHashMap} for thread-safe concurrent access.
     */
    @Builder.Default
    private Set<String> enrolledLearnerIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * FIFO waitlist queue — accessed exclusively inside the per-course ReentrantLock.
     */
    @Builder.Default
    private Queue<String> waitlistQueue = new LinkedList<>();

    /**
     * Prerequisite courseIds (populated but not yet enforced — criteria TBD).
     * Not enforced by {@code EnrollmentService}; check {@code Learner.prerequisiteMet} instead.
     */
    @Builder.Default
    private List<String> prerequisites = new ArrayList<>();

    public int getEnrolledCount() {
        return enrolledLearnerIds.size();
    }

    public int getWaitlistCount() {
        return waitlistQueue.size();
    }

    public boolean hasCapacity() {
        return enrolledLearnerIds.size() < maxCapacity;
    }
}

