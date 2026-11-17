package com.charles.courseenrollment.exception;

/**
 * Thrown when a requested course cannot be found in the in-memory store.
 */
public class CourseNotFoundException extends RuntimeException {

    public CourseNotFoundException(String courseId) {
        super("Course not found with ID: " + courseId);
    }
}
