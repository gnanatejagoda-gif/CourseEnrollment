package com.charles.courseenrollment.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Public-facing response DTO for course queries.
 */
@Schema(description = "Safe public projection of course details and enrollment capacity")
public record CourseView(
        @Schema(description = "Course identifier", example = "C002")
        String courseId,

        @Schema(description = "Title of the course", example = "Spring Boot")
        String title,

        @Schema(description = "Course summary description", example = "Building REST microservices with Spring Boot.")
        String description,

        @Schema(description = "Maximum enrollment capacity", example = "2")
        int maxCapacity,

        @Schema(description = "Number of currently enrolled learners", example = "1")
        int enrolledCount,

        @Schema(description = "Number of learners in waitlist queue", example = "0")
        int waitlistCount,

        @Schema(description = "Remaining available seats", example = "1")
        int availableSeats,

        @Schema(description = "List of prerequisite course IDs required for enrollment", example = "[\"C001\"]")
        List<String> prerequisites
) {
    public static CourseView from(Course course) {
        return new CourseView(
                course.getCourseId(),
                course.getTitle(),
                course.getDescription(),
                course.getMaxCapacity(),
                course.getEnrolledCount(),
                course.getWaitlistCount(),
                Math.max(0, course.getMaxCapacity() - course.getEnrolledCount()),
                List.copyOf(course.getPrerequisites())
        );
    }
}
