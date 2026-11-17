package com.charles.courseenrollment.controller;

import com.charles.courseenrollment.exception.CourseNotFoundException;
import com.charles.courseenrollment.exception.GlobalExceptionHandler;
import com.charles.courseenrollment.model.Course;
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
class CourseControllerTest {

    @Mock
    private EnrollmentService enrollmentService;

    @InjectMocks
    private CourseController courseController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(courseController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/courses - 200 OK returns list of courses as CourseView projections")
    void shouldReturnAllCourses() throws Exception {
        Course c1 = Course.builder().courseId("C001").title("Java Basics").maxCapacity(3).build();
        Course c2 = Course.builder().courseId("C002").title("Spring Boot").maxCapacity(2).build();

        when(enrollmentService.getAllCourses()).thenReturn(List.of(c1, c2));

        mockMvc.perform(get("/api/courses").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].courseId").value("C001"))
                .andExpect(jsonPath("$[0].title").value("Java Basics"))
                .andExpect(jsonPath("$[0].availableSeats").value(3))
                .andExpect(jsonPath("$[1].courseId").value("C002"));

        verify(enrollmentService).getAllCourses();
    }

    @Test
    @DisplayName("GET /api/courses/{courseId} - 200 OK returns CourseView")
    void shouldReturnCourseById() throws Exception {
        Course course = Course.builder()
                .courseId("C001")
                .title("Java Basics")
                .description("Intro")
                .maxCapacity(5)
                .build();

        when(enrollmentService.getCourse("C001")).thenReturn(course);

        mockMvc.perform(get("/api/courses/C001").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value("C001"))
                .andExpect(jsonPath("$.title").value("Java Basics"))
                .andExpect(jsonPath("$.maxCapacity").value(5))
                .andExpect(jsonPath("$.availableSeats").value(5));
    }

    @Test
    @DisplayName("GET /api/courses/{courseId} - 404 Not Found when course does not exist")
    void shouldReturn404WhenCourseNotFound() throws Exception {
        when(enrollmentService.getCourse("C999")).thenThrow(new CourseNotFoundException("C999"));

        mockMvc.perform(get("/api/courses/C999").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("Course not found with ID: C999")));
    }
}
