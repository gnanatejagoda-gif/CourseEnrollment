package com.charles.courseenrollment.controller;

import com.charles.courseenrollment.model.CourseView;
import com.charles.courseenrollment.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for read-only course information.
 */
@Tag(name = "Courses", description = "Endpoints for browsing course catalog and enrollment capacity")
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final EnrollmentService enrollmentService;

    public CourseController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    /**
     * List all available courses with enrollment summary counts.
     */
    @Operation(
            summary = "List all courses",
            description = "Retrieves all available courses with summary metadata: max capacity, enrolled count, waitlist count, and remaining available seats."
    )
    @ApiResponse(responseCode = "200", description = "List of all courses returned successfully")
    @GetMapping
    public ResponseEntity<List<CourseView>> getAllCourses() {
        List<CourseView> views = enrollmentService.getAllCourses()
                .stream()
                .map(CourseView::from)
                .toList();
        return ResponseEntity.ok(views);
    }

    /**
     * Get summary details for a specific course.
     */
    @Operation(
            summary = "Get course by ID",
            description = "Retrieves course details, remaining seat count, waitlist depth, and prerequisite courses by course ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Course found and returned"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @GetMapping("/{courseId}")
    public ResponseEntity<CourseView> getCourse(
            @Parameter(description = "Unique ID of the course", example = "C002")
            @PathVariable String courseId
    ) {
        return ResponseEntity.ok(CourseView.from(enrollmentService.getCourse(courseId)));
    }
}
