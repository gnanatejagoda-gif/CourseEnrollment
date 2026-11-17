package com.charles.courseenrollment.exception;

/**
 * Thrown when a requested learner cannot be found in the in-memory store.
 */
public class LearnerNotFoundException extends RuntimeException {

    public LearnerNotFoundException(String learnerId) {
        super("Learner not found with ID: " + learnerId);
    }
}
