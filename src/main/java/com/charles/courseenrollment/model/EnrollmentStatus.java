package com.charles.courseenrollment.model;

/**
 * Machine-readable status codes returned in every EnrollmentResponse.
 */
public enum EnrollmentStatus {

    /** Learner was successfully enrolled in the course. */
    ENROLLED,

    /** Learner attempted to enroll but is already enrolled in this course. */
    ALREADY_ENROLLED,

    /** Course is at full capacity — learner was added to the FIFO waitlist. */
    WAITLISTED,

    /**
     * Learner does not meet the prerequisite requirement (prerequisiteMet == false).
     * Learner is neither enrolled nor added to the waitlist.
     */
    NOT_ELIGIBLE,

    /** Learner was successfully removed from the course. */
    UNENROLLED,

    /**
     * A waitlisted learner was automatically promoted to enrolled status
     * after another learner unenrolled, freeing a seat.
     */
    PROMOTED
}
