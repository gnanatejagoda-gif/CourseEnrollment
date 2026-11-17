package com.charles.courseenrollment.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Typed request DTO for enrollment and unenrollment operations.
 */
@Schema(description = "Payload required to enroll or unenroll a learner from a course")
public record EnrollmentRequest(
        @Schema(
                description = "Unique identifier of the learner",
                example = "L001",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "learnerId must not be blank")
        String learnerId,

        @Schema(
                description = "Unique identifier of the target course",
                example = "C002",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "courseId must not be blank")
        String courseId
) {}
