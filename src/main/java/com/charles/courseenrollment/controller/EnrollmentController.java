package com.charles.courseenrollment.controller;

import com.charles.courseenrollment.model.EnrollmentRequest;
import com.charles.courseenrollment.model.EnrollmentResponse;
import com.charles.courseenrollment.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing dedicated enroll and unenroll endpoints.
 */
@Tag(name = "Enrollment", description = "Endpoints for learner enrollment and unenrollment with automatic waitlist promotion")
@RestController
@RequestMapping("/api/enrollment")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    /**
     * Enroll a learner in a course.
     */
    @Operation(
            summary = "Enroll a learner in a course",
            description = """
                    Attempts to enroll a learner in the specified course:
                    - **Available Capacity:** If seats remain and prerequisites are met, status is `ENROLLED`.
                    - **Full Capacity:** If course is full and prerequisites are met, learner is queued with status `WAITLISTED`.
                    - **Duplicate Enroll:** If learner is already enrolled, status is `ALREADY_ENROLLED`.
                    - **Prerequisite Not Met:** If learner is ineligible (`prerequisiteMet == false`), status is `NOT_ELIGIBLE`.
                    - **Concurrency Protection:** Protected by per-course `ReentrantLock` preventing race conditions.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Enrollment processed successfully (check 'code' field for specific outcome)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EnrollmentResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Enrolled",
                                            summary = "Seat available and enrolled",
                                            value = """
                                                    {
                                                      "learnerId": "L001",
                                                      "courseId": "C002",
                                                      "code": "ENROLLED",
                                                      "status": "ENROLLED",
                                                      "message": "Learner L001 successfully enrolled in course C002."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Waitlisted",
                                            summary = "Course full, added to waitlist queue",
                                            value = """
                                                    {
                                                      "learnerId": "L003",
                                                      "courseId": "C002",
                                                      "code": "WAITLISTED",
                                                      "status": "WAITLISTED",
                                                      "message": "Course C002 is at full capacity. Learner L003 has been added to the waitlist (position 1)."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Already Enrolled",
                                            summary = "Learner already enrolled in this course",
                                            value = """
                                                    {
                                                      "learnerId": "L001",
                                                      "courseId": "C002",
                                                      "code": "ALREADY_ENROLLED",
                                                      "status": "ALREADY_ENROLLED",
                                                      "message": "Learner L001 is already enrolled in course C002."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Not Eligible",
                                            summary = "Prerequisite criteria not met",
                                            value = """
                                                    {
                                                      "learnerId": "L002",
                                                      "courseId": "C002",
                                                      "code": "NOT_ELIGIBLE",
                                                      "status": "NOT_ELIGIBLE",
                                                      "message": "Learner L002 does not meet the prerequisites for course C002."
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request payload (e.g. blank learnerId or courseId)"),
            @ApiResponse(responseCode = "404", description = "Course or learner not found")
    })
    @PostMapping("/enroll")
    public ResponseEntity<EnrollmentResponse> enroll(@RequestBody @Valid EnrollmentRequest request) {
        EnrollmentResponse response = enrollmentService.enroll(request.learnerId(), request.courseId());
        return ResponseEntity.ok(response);
    }

    /**
     * Unenroll a learner from a course.
     */
    @Operation(
            summary = "Unenroll a learner from a course",
            description = """
                    Removes an enrolled learner from the course.
                    - If waitlisted candidates exist, the next learner in FIFO order is **automatically promoted** to enrolled status.
                    - If the learner is not currently enrolled, returns `400 Bad Request`.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Learner successfully unenrolled (promoted learner indicated in message if applicable)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EnrollmentResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Unenrolled with Promotion",
                                            summary = "Dropped seat filled by waitlisted learner",
                                            value = """
                                                    {
                                                      "learnerId": "L001",
                                                      "courseId": "C002",
                                                      "code": "UNENROLLED",
                                                      "status": "UNENROLLED",
                                                      "message": "Learner L001 unenrolled from course C002. Learner L003 promoted from the waitlist."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Unenrolled without Promotion",
                                            summary = "Waitlist empty, seat vacated",
                                            value = """
                                                    {
                                                      "learnerId": "L001",
                                                      "courseId": "C002",
                                                      "code": "UNENROLLED",
                                                      "status": "UNENROLLED",
                                                      "message": "Learner L001 successfully unenrolled from course C002."
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Learner is not currently enrolled in the course or invalid payload"),
            @ApiResponse(responseCode = "404", description = "Course or learner not found")
    })
    @PostMapping("/unenroll")
    public ResponseEntity<EnrollmentResponse> unenroll(@RequestBody @Valid EnrollmentRequest request) {
        EnrollmentResponse response = enrollmentService.unenroll(request.learnerId(), request.courseId());
        return ResponseEntity.ok(response);
    }
}
