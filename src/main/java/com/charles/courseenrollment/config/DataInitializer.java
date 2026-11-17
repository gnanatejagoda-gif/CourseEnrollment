package com.charles.courseenrollment.config;

import com.charles.courseenrollment.model.Course;
import com.charles.courseenrollment.model.Learner;
import com.charles.courseenrollment.repository.CourseRepository;
import com.charles.courseenrollment.repository.LearnerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Seeds the in-memory stores with sample courses and learners on application startup.
 *
 * <p>Course catalog:
 * <ul>
 *   <li>C001 — Java Basics         (capacity 3, no prerequisites)</li>
 *   <li>C002 — Spring Boot         (capacity 2, requires C001)</li>
 *   <li>C003 — Advanced Microservices (capacity 2, requires C001 + C002)</li>
 * </ul>
 *
 * <p>Learner roster:
 * <ul>
 *   <li>L001 — Alice   (prerequisiteMet = true)</li>
 *   <li>L002 — Bob     (prerequisiteMet = false)</li>
 *   <li>L003 — Charlie (prerequisiteMet = true)</li>
 * </ul>
 */
@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner seedData(CourseRepository courseRepository,
                                      LearnerRepository learnerRepository) {
        return args -> {

            // ---- Courses ----
            courseRepository.save(Course.builder()
                    .courseId("C001")
                    .title("Java Basics")
                    .description("Introduction to Java programming language fundamentals.")
                    .maxCapacity(3)
                    .prerequisites(List.of())
                    .build());

            courseRepository.save(Course.builder()
                    .courseId("C002")
                    .title("Spring Boot")
                    .description("Building REST microservices with Spring Boot.")
                    .maxCapacity(2)
                    .prerequisites(List.of("C001"))
                    .build());

            courseRepository.save(Course.builder()
                    .courseId("C003")
                    .title("Advanced Microservices")
                    .description("Patterns and practices for production-grade microservices.")
                    .maxCapacity(2)
                    .prerequisites(List.of("C001", "C002"))
                    .build());

            // ---- Learners ----
            learnerRepository.save(Learner.builder()
                    .learnerId("L001")
                    .name("Alice")
                    .email("alice@example.com")
                    .prerequisiteMet(true)
                    .build());

            learnerRepository.save(Learner.builder()
                    .learnerId("L002")
                    .name("Bob")
                    .email("bob@example.com")
                    .prerequisiteMet(false)
                    .build());

            learnerRepository.save(Learner.builder()
                    .learnerId("L003")
                    .name("Charlie")
                    .email("charlie@example.com")
                    .prerequisiteMet(true)
                    .build());

            System.out.println("✅ DataInitializer: seeded 3 courses and 3 learners.");
        };
    }
}
