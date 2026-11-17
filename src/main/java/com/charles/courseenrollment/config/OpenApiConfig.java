package com.charles.courseenrollment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3 / Swagger configuration bean.
 *
 * <p>Customizes the interactive Swagger UI page accessible at
 * <a href="http://localhost:8080/swagger-ui.html">http://localhost:8080/swagger-ui.html</a>.</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI courseEnrollmentOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Course Enrollment Microservice API")
                        .version("1.0.0")
                        .description("""
                                ## Course Enrollment & Waitlist Microservice
                                
                                A Spring Boot RESTful microservice providing thread-safe course enrollment
                                and unenrollment capabilities backed by in-memory data structures.
                                
                                ### Key Capabilities:
                                - **Capacity Management:** Enrolls learners up to course capacity.
                                - **FIFO Waitlist Queue:** Automatically queues learners when capacity is reached.
                                - **Automatic FIFO Promotion:** When a learner drops/unenrolls, the next waiting learner is immediately promoted to enrolled status.
                                - **Prerequisite Eligibility:** Validates learner eligibility before enrollment or waitlisting.
                                - **Concurrent Race-Free Processing:** Per-course fair `ReentrantLock` guarantees zero duplicate seats or lost promotions.
                                """)
                        .contact(new Contact()
                                .name("Engineering Team")
                                .email("engineering@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server")
                ))
                .tags(List.of(
                        new Tag().name("Enrollment").description("Operations for learner enrollment and unenrollment with automatic waitlist promotion"),
                        new Tag().name("Courses").description("Course catalog and enrollment capacity inspection"),
                        new Tag().name("Learners").description("Learner roster and course participation details")
                ));
    }
}
