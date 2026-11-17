package com.charles.courseenrollment.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

/**
 * Public-facing response DTO for learner queries.
 */
@Schema(description = "Safe public projection of learner profile and course participation")
public record LearnerView(
        @Schema(description = "Unique learner identifier", example = "L001")
        String learnerId,

        @Schema(description = "Full name of the learner", example = "Alice")
        String name,

        @Schema(description = "Course IDs currently enrolled in", example = "[\"C001\", \"C002\"]")
        Set<String> enrolledCourseIds,

        @Schema(description = "Course IDs currently waitlisted for", example = "[]")
        Set<String> waitlistedCourseIds
) {
    public static LearnerView from(Learner learner) {
        return new LearnerView(
                learner.getLearnerId(),
                learner.getName(),
                Set.copyOf(learner.getEnrolledCourseIds()),
                Set.copyOf(learner.getWaitlistedCourseIds())
        );
    }
}
