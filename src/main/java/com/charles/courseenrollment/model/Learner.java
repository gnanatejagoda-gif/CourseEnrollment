package com.charles.courseenrollment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Learner {

    private String learnerId;
    private String name;
    private String email;

    /**
     * Simple eligibility flag. Actual prerequisite criteria is TBD.
     * true  = learner meets prerequisites and may enroll or be waitlisted.
     * false = learner is blocked from enrolling or joining the waitlist.
     */
    private boolean prerequisiteMet;

    @Builder.Default
    private Set<String> enrolledCourseIds = new HashSet<>();

    @Builder.Default
    private Set<String> waitlistedCourseIds = new HashSet<>();

    public boolean isEnrolledIn(String courseId) {
        return enrolledCourseIds.contains(courseId);
    }

    public boolean isWaitlistedFor(String courseId) {
        return waitlistedCourseIds.contains(courseId);
    }
}
