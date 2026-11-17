package com.charles.courseenrollment.exception;

/**
 * Thrown for business-rule violations during enrollment operations,
 * e.g. attempting to unenroll from a course the learner is not enrolled in.
 */
public class EnrollmentException extends RuntimeException {

    public EnrollmentException(String message) {
        super(message);
    }
}
