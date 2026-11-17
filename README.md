# Course Enrollment Microservice

A Spring Boot RESTful microservice for learner course enrollment and unenrollment, backed by in-memory data structures with capacity limits, FIFO waitlisting, prerequisite eligibility checks, and race-free concurrency handling.

---

## 📌 Architecture & Features

* **In-Memory Storage:** `CourseRepository` and `LearnerRepository` backed by thread-safe `ConcurrentHashMap` stores.
* **Layered Architecture:** Model, Repository, Service, Controller, and Centralized Exception Handling layers.
* **Capacity Management:** Enrolls learners up to course capacity.
* **FIFO Waitlist Queue:** When course capacity is reached, eligible learners are queued in a FIFO `LinkedList` waitlist.
* **Automatic Waitlist Promotion:** When an enrolled learner unenrolls, the next waitlisted learner is **automatically promoted** into the course.
* **Prerequisite Eligibility:** Validates learner eligibility (`prerequisiteMet`) before enrollment or waitlisting. Ineligible learners are rejected with `NOT_ELIGIBLE`.
* **Duplicate Prevention:** Re-enrolling in the same course returns `ALREADY_ENROLLED` with zero state corruption.
* **Concurrency Protection:** Uses per-course fair **`ReentrantLock`** (`java.util.concurrent.locks.ReentrantLock`) to ensure race-condition-free enrollment under concurrent load.
* **Interactive Documentation:** Integrated **Swagger UI / OpenAPI 3** for browser-based testing.

---

## ⚙️ Prerequisites

* **Java:** JDK 21 (Temurin, Oracle, or IntelliJ's bundled JBR 21)
* **Build Tool:** Maven (or bundled `./mvnw` / `mvnw.cmd` wrapper)
* **IDE:** IntelliJ IDEA, Eclipse, or VS Code

---

## 🛠️ Build and Run Commands

Open a terminal in the project root directory:

### 1. Ensure `JAVA_HOME` is Configured (PowerShell Example)
```powershell
# Point to your JDK 21 directory (example using IntelliJ's bundled JDK)
```

### 2. Build and Run Tests
```powershell
.\mvnw.cmd clean package
```
*Executes all unit and concurrency tests in `EnrollmentServiceTest` and `EnrollmentControllerTest`.*

### 3. Run the Application
```powershell
.\mvnw.cmd spring-boot:run
```
*(Or in IntelliJ IDEA: open `CourseEnrollmentApplication.java` and click **Run▶**).*

The server will start on **`http://localhost:8080`**.

---

## 🌐 Swagger UI (Interactive API Documentation)

Once the application is running, open your browser to:

* **Interactive Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **OpenAPI 3 JSON Spec:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

You can inspect all schemas and execute API calls directly from the Swagger UI using the pre-enabled **"Try it out"** feature.

---

## 📦 Seed Data Loaded on Startup

`DataInitializer.java` automatically seeds the in-memory store on startup:

### Course Catalog
| Course ID | Title | Capacity | Prerequisites |
|---|---|---|---|
| `C001` | Java Basics | 3 seats | None |
| `C002` | Spring Boot | 2 seats | `C001` |
| `C003` | Advanced Microservices | 2 seats | `C001`, `C002` |

### Learner Roster
| Learner ID | Name | `prerequisiteMet` | Eligibility |
|---|---|---|---|
| `L001` | Alice | `true` | Eligible |
| `L002` | Bob | `false` | Ineligible (fails prerequisite) |
| `L003` | Charlie | `true` | Eligible |

---

## 🧪 Testing the Endpoints (Step-by-Step)

### 1. Browse All Courses
```powershell
curl -X GET "http://localhost:8080/api/courses"
```

### 2. Successful Enrollment (Available Capacity)
Enroll Alice (`L001`) into Spring Boot (`C002`):
```powershell
curl -X POST "http://localhost:8080/api/enrollment/enroll" `
  -H "Content-Type: application/json" `
  -d '{"learnerId": "L001", "courseId": "C002"}'
```
**Response (200 OK):**
```json
{
  "learnerId": "L001",
  "courseId": "C002",
  "code": "ENROLLED",
  "status": "ENROLLED",
  "message": "Learner L001 successfully enrolled in course C002."
}
```

### 3. Duplicate Enrollment Prevention
Attempting to enroll Alice (`L001`) into `C002` again:
```powershell
curl -X POST "http://localhost:8080/api/enrollment/enroll" `
  -H "Content-Type: application/json" `
  -d '{"learnerId": "L001", "courseId": "C002"}'
```
**Response (200 OK):**
```json
{
  "learnerId": "L001",
  "courseId": "C002",
  "code": "ALREADY_ENROLLED",
  "status": "ALREADY_ENROLLED",
  "message": "Learner L001 is already enrolled in course C002."
}
```

### 4. Prerequisite Failure Test
Attempting to enroll Bob (`L002`, `prerequisiteMet = false`) into `C002`:
```powershell
curl -X POST "http://localhost:8080/api/enrollment/enroll" `
  -H "Content-Type: application/json" `
  -d '{"learnerId": "L002", "courseId": "C002"}'
```
**Response (200 OK):**
```json
{
  "learnerId": "L002",
  "courseId": "C002",
  "code": "NOT_ELIGIBLE",
  "status": "NOT_ELIGIBLE",
  "message": "Learner L002 does not meet the prerequisites for course C002."
}
```

### 5. Full Capacity & Waitlist Queue
Enroll Charlie (`L003`) into `C002` (takes the 2nd and final seat):
```powershell
curl -X POST "http://localhost:8080/api/enrollment/enroll" `
  -H "Content-Type: application/json" `
  -d '{"learnerId": "L003", "courseId": "C002"}'
```
`C002` is now at full capacity (2/2). Any subsequent eligible learner will be queued with code `WAITLISTED`.

### 6. Un-Enroll with Automatic Waitlist Promotion
When an enrolled learner unenrolls:
```powershell
curl -X POST "http://localhost:8080/api/enrollment/unenroll" `
  -H "Content-Type: application/json" `
  -d '{"learnerId": "L001", "courseId": "C002"}'
```
**Response (200 OK):**
```json
{
  "learnerId": "L001",
  "courseId": "C002",
  "code": "UNENROLLED",
  "status": "UNENROLLED",
  "message": "Learner L001 unenrolled from course C002. Learner L003 promoted from the waitlist."
}
```

---

## 📁 Project Structure

```
src/
├── main/
│   ├── java/com/charles/courseenrollment/
│   │   ├── CourseEnrollmentApplication.java  # Main Spring Boot entry point
│   │   ├── config/
│   │   │   ├── DataInitializer.java          # In-memory startup seed data
│   │   │   └── OpenApiConfig.java            # OpenAPI 3 / Swagger UI configuration
│   │   ├── controller/
│   │   │   ├── CourseController.java         # GET /api/courses
│   │   │   ├── EnrollmentController.java     # POST /api/enrollment/enroll & unenroll
│   │   │   └── LearnerController.java        # GET /api/learners
│   │   ├── exception/
│   │   │   ├── CourseNotFoundException.java  # 404 handler
│   │   │   ├── EnrollmentException.java      # 400 handler
│   │   │   ├── GlobalExceptionHandler.java   # Centralized @RestControllerAdvice
│   │   │   └── LearnerNotFoundException.java # 404 handler
│   │   ├── model/
│   │   │   ├── Course.java                   # In-memory course entity
│   │   │   ├── CourseView.java               # Safe projection DTO
│   │   │   ├── EnrollmentRequest.java        # @NotBlank validated DTO
│   │   │   ├── EnrollmentResponse.java       # Machine-readable status response
│   │   │   ├── EnrollmentStatus.java         # Status enum
│   │   │   ├── Learner.java                  # In-memory learner entity
│   │   │   └── LearnerView.java              # Safe projection DTO
│   │   ├── repository/
│   │   │   ├── CourseRepository.java         # ConcurrentHashMap course store
│   │   │   └── LearnerRepository.java        # ConcurrentHashMap learner store
│   │   └── service/
│   │       └── EnrollmentService.java        # ReentrantLock business logic
│   └── resources/
│       └── application.properties            # Swagger & application configuration
└── test/
    └── java/com/charles/courseenrollment/
        ├── controller/
        │   ├── CourseControllerTest.java     # JUnit 5 MockMvc unit tests
        │   ├── EnrollmentControllerTest.java # JUnit 5 MockMvc unit tests
        │   └── LearnerControllerTest.java    # JUnit 5 MockMvc unit tests
        └── service/
            └── EnrollmentServiceTest.java    # 16 unit & multi-threaded concurrency tests
```
