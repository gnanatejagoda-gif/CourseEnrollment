package com.charles.courseenrollment.controller;

import com.charles.courseenrollment.exception.CourseNotFoundException;
import com.charles.courseenrollment.exception.EnrollmentException;
import com.charles.courseenrollment.exception.GlobalExceptionHandler;
import com.charles.courseenrollment.model.EnrollmentRequest;
import com.charles.courseenrollment.model.EnrollmentResponse;
import com.charles.courseenrollment.model.EnrollmentStatus;
import com.charles.courseenrollment.service.EnrollmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Standard JUnit 5 unit tests for {@link EnrollmentController}.
 *
 * <p>Follows standard JUnit 5 & Mockito test guidelines:
 * <ul>
 *   <li>Uses {@link ExtendWith @ExtendWith(MockitoExtension.class)} for pure unit isolation</li>
 *   <li>Uses {@link Mock @Mock} for mock dependencies and {@link InjectMocks @InjectMocks} for the controller</li>
 *   <li>Uses {@link MockMvcBuilders#standaloneSetup} with {@link GlobalExceptionHandler} to test HTTP mappings,
 *       status codes, error payloads, and JSON serialization without Spring context overhead</li>
 *   <li>Also includes direct method unit test assertions</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class EnrollmentControllerTest {

    @Mock
    private EnrollmentService enrollmentService;

    @InjectMocks
    private EnrollmentController enrollmentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(enrollmentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // =========================================================================
    // Direct Method Unit Tests (Pure JUnit 5)
    // =========================================================================
    @Nested
    @DisplayName("Direct Method Unit Tests")
    class DirectMethodTests {

        @Test
        @DisplayName("enroll() returns 200 OK with EnrollmentResponse")
        void shouldReturnEnrollmentResponseDirectly() {
            EnrollmentRequest request = new EnrollmentRequest("L1", "C101");
            EnrollmentResponse expected = EnrollmentResponse.of("L1", "C101",
                    EnrollmentStatus.ENROLLED, "Successfully enrolled in course C101.");

            when(enrollmentService.enroll("L1", "C101")).thenReturn(expected);

            ResponseEntity<EnrollmentResponse> response = enrollmentController.enroll(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo("ENROLLED");
            assertThat(response.getBody().getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
            assertThat(response.getBody().getLearnerId()).isEqualTo("L1");
            assertThat(response.getBody().getCourseId()).isEqualTo("C101");

            verify(enrollmentService).enroll("L1", "C101");
        }

        @Test
        @DisplayName("unenroll() returns 200 OK with EnrollmentResponse")
        void shouldReturnUnenrollResponseDirectly() {
            EnrollmentRequest request = new EnrollmentRequest("L1", "C101");
            EnrollmentResponse expected = EnrollmentResponse.of("L1", "C101",
                    EnrollmentStatus.UNENROLLED, "Successfully unenrolled from course C101.");

            when(enrollmentService.unenroll("L1", "C101")).thenReturn(expected);

            ResponseEntity<EnrollmentResponse> response = enrollmentController.unenroll(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo("UNENROLLED");
            assertThat(response.getBody().getStatus()).isEqualTo(EnrollmentStatus.UNENROLLED);

            verify(enrollmentService).unenroll("L1", "C101");
        }
    }

    // =========================================================================
    // MockMvc HTTP & Serialization Tests (Standalone Setup)
    // =========================================================================
    @Nested
    @DisplayName("MockMvc HTTP Endpoint Tests")
    class MockMvcEndpointTests {

        @Test
        @DisplayName("POST /api/enrollment/enroll - 200 OK on valid enrollment")
        void shouldReturn200OnSuccessfulEnrollment() throws Exception {
            EnrollmentResponse expected = EnrollmentResponse.of("L1", "C101",
                    EnrollmentStatus.ENROLLED, "Successfully enrolled in course C101.");

            when(enrollmentService.enroll(eq("L1"), eq("C101"))).thenReturn(expected);

            String requestJson = """
                    {
                        "learnerId": "L1",
                        "courseId": "C101"
                    }
                    """;

            mockMvc.perform(post("/api/enrollment/enroll")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("ENROLLED"))
                    .andExpect(jsonPath("$.status").value("ENROLLED"))
                    .andExpect(jsonPath("$.learnerId").value("L1"))
                    .andExpect(jsonPath("$.courseId").value("C101"))
                    .andExpect(jsonPath("$.message").value(containsString("Successfully enrolled")));
        }

        @Test
        @DisplayName("POST /api/enrollment/enroll - 200 OK when ALREADY_ENROLLED returned")
        void shouldReturn200WithAlreadyEnrolledCode() throws Exception {
            EnrollmentResponse expected = EnrollmentResponse.of("L1", "C101",
                    EnrollmentStatus.ALREADY_ENROLLED, "Learner L1 is already enrolled in course C101.");

            when(enrollmentService.enroll(eq("L1"), eq("C101"))).thenReturn(expected);

            String requestJson = """
                    {
                        "learnerId": "L1",
                        "courseId": "C101"
                    }
                    """;

            mockMvc.perform(post("/api/enrollment/enroll")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("ALREADY_ENROLLED"))
                    .andExpect(jsonPath("$.status").value("ALREADY_ENROLLED"))
                    .andExpect(jsonPath("$.message").value(containsString("already enrolled")));
        }

        @Test
        @DisplayName("POST /api/enrollment/enroll - 200 OK when WAITLISTED returned")
        void shouldReturn200WithWaitlistedCode() throws Exception {
            EnrollmentResponse expected = EnrollmentResponse.of("L2", "C101",
                    EnrollmentStatus.WAITLISTED, "Course C101 is at full capacity. Learner L2 has been added to the waitlist.");

            when(enrollmentService.enroll(eq("L2"), eq("C101"))).thenReturn(expected);

            String requestJson = """
                    {
                        "learnerId": "L2",
                        "courseId": "C101"
                    }
                    """;

            mockMvc.perform(post("/api/enrollment/enroll")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("WAITLISTED"))
                    .andExpect(jsonPath("$.status").value("WAITLISTED"));
        }

        @Test
        @DisplayName("POST /api/enrollment/unenroll - 200 OK on successful unenrollment")
        void shouldReturn200OnSuccessfulUnenroll() throws Exception {
            EnrollmentResponse expected = EnrollmentResponse.of("L1", "C101",
                    EnrollmentStatus.UNENROLLED, "Learner L1 successfully unenrolled from course C101.");

            when(enrollmentService.unenroll(eq("L1"), eq("C101"))).thenReturn(expected);

            String requestJson = """
                    {
                        "learnerId": "L1",
                        "courseId": "C101"
                    }
                    """;

            mockMvc.perform(post("/api/enrollment/unenroll")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("UNENROLLED"))
                    .andExpect(jsonPath("$.status").value("UNENROLLED"))
                    .andExpect(jsonPath("$.message").value(containsString("successfully unenrolled")));
        }

        @Test
        @DisplayName("POST /api/enrollment/unenroll - 400 Bad Request when learner is not enrolled")
        void shouldReturn400WhenUnenrollingNonEnrolledLearner() throws Exception {
            when(enrollmentService.unenroll(eq("L1"), eq("C101")))
                    .thenThrow(new EnrollmentException("Learner L1 is not currently enrolled in course C101."));

            String requestJson = """
                    {
                        "learnerId": "L1",
                        "courseId": "C101"
                    }
                    """;

            mockMvc.perform(post("/api/enrollment/unenroll")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("not currently enrolled")));
        }

        @Test
        @DisplayName("POST /api/enrollment/enroll - 404 Not Found when course does not exist")
        void shouldReturn404WhenCourseNotFound() throws Exception {
            when(enrollmentService.enroll(eq("L1"), eq("C999")))
                    .thenThrow(new CourseNotFoundException("C999"));

            String requestJson = """
                    {
                        "learnerId": "L1",
                        "courseId": "C999"
                    }
                    """;

            mockMvc.perform(post("/api/enrollment/enroll")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value(containsString("Course not found with ID: C999")));
        }
    }
}
