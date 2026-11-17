package com.charles.courseenrollment.controller;

import com.charles.courseenrollment.model.LearnerView;
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
 * REST controller for read-only learner information.
 */
@Tag(name = "Learners", description = "Endpoints for viewing learner roster and enrolled/waitlisted courses")
@RestController
@RequestMapping("/api/learners")
public class LearnerController {

    private final EnrollmentService enrollmentService;

    public LearnerController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    /**
     * List all registered learners with their enrolled and waitlisted course IDs.
     */
    @Operation(
            summary = "List all learners",
            description = "Retrieves all registered learners and their active enrolled and waitlisted course memberships (excludes sensitive email/prerequisite internals)."
    )
    @ApiResponse(responseCode = "200", description = "List of all learners returned successfully")
    @GetMapping
    public ResponseEntity<List<LearnerView>> getAllLearners() {
        List<LearnerView> views = enrollmentService.getAllLearners()
                .stream()
                .map(LearnerView::from)
                .toList();
        return ResponseEntity.ok(views);
    }

    /**
     * Get enrollment state for a specific learner.
     */
    @Operation(
            summary = "Get learner by ID",
            description = "Retrieves learner profile and active enrolled and waitlisted course IDs."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Learner found and returned"),
            @ApiResponse(responseCode = "404", description = "Learner not found")
    })
    @GetMapping("/{learnerId}")
    public ResponseEntity<LearnerView> getLearner(
            @Parameter(description = "Unique ID of the learner", example = "L001")
            @PathVariable String learnerId
    ) {
        return ResponseEntity.ok(LearnerView.from(enrollmentService.getLearner(learnerId)));
    }
}
