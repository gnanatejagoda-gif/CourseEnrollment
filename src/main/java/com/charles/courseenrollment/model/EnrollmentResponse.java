package com.charles.courseenrollment.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response payload describing the result of an enrollment or unenrollment request")
public class EnrollmentResponse {

    @Schema(description = "ID of the learner", example = "L001")
    private String learnerId;

    @Schema(description = "ID of the course", example = "C002")
    private String courseId;

    @Schema(
            description = "Machine-readable status code",
            example = "ENROLLED",
            allowableValues = {"ENROLLED", "ALREADY_ENROLLED", "WAITLISTED", "NOT_ELIGIBLE", "UNENROLLED", "PROMOTED"}
    )
    private String code;

    @Schema(description = "Strongly typed enrollment status enum", example = "ENROLLED")
    private EnrollmentStatus status;

    @Schema(description = "Descriptive outcome message", example = "Learner L001 successfully enrolled in course C002.")
    private String message;

    public static EnrollmentResponse of(String learnerId, String courseId,
                                        EnrollmentStatus status, String message) {
        return EnrollmentResponse.builder()
                .learnerId(learnerId)
                .courseId(courseId)
                .code(status.name())
                .status(status)
                .message(message)
                .build();
    }
}
