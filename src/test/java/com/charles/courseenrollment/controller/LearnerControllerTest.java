package com.charles.courseenrollment.controller;

import com.charles.courseenrollment.exception.GlobalExceptionHandler;
import com.charles.courseenrollment.exception.LearnerNotFoundException;
import com.charles.courseenrollment.model.Learner;
import com.charles.courseenrollment.service.EnrollmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LearnerControllerTest {

    @Mock
    private EnrollmentService enrollmentService;

    @InjectMocks
    private LearnerController learnerController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(learnerController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/learners - 200 OK returns list of learners as LearnerView projections without email")
    void shouldReturnAllLearnersWithoutSensitiveData() throws Exception {
        Learner l1 = Learner.builder().learnerId("L1").name("Alice").email("alice@example.com").build();
        Learner l2 = Learner.builder().learnerId("L2").name("Bob").email("bob@example.com").build();

        when(enrollmentService.getAllLearners()).thenReturn(List.of(l1, l2));

        mockMvc.perform(get("/api/learners").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].learnerId").value("L1"))
                .andExpect(jsonPath("$[0].name").value("Alice"))
                .andExpect(jsonPath("$[0].email").doesNotExist()) // Privacy projection verified
                .andExpect(jsonPath("$[1].learnerId").value("L2"))
                .andExpect(jsonPath("$[1].email").doesNotExist());

        verify(enrollmentService).getAllLearners();
    }

    @Test
    @DisplayName("GET /api/learners/{learnerId} - 200 OK returns LearnerView")
    void shouldReturnLearnerById() throws Exception {
        Learner learner = Learner.builder()
                .learnerId("L1")
                .name("Alice")
                .email("alice@example.com")
                .build();

        when(enrollmentService.getLearner("L1")).thenReturn(learner);

        mockMvc.perform(get("/api/learners/L1").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learnerId").value("L1"))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/learners/{learnerId} - 404 Not Found when learner does not exist")
    void shouldReturn404WhenLearnerNotFound() throws Exception {
        when(enrollmentService.getLearner("L999")).thenThrow(new LearnerNotFoundException("L999"));

        mockMvc.perform(get("/api/learners/L999").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("Learner not found with ID: L999")));
    }
}
