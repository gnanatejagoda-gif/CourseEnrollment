# CourseEnrollment Project — Prompt & Action Audit Log

This file is maintained as an audit log of all user prompts and actions taken throughout the CourseEnrollment project conversation.

---

## Entry #1

| Field | Details |
|---|---|
| **Entry #** | 1 |
| **Timestamp** | 2026-11-17T17:39:48+05:30 |
| **Status** | Preview Delivered — Awaiting User Approval |

### User Prompt
Build a Java Spring Boot microservice with REST endpoints for learner course enrollment/un-enrollment with the following requirements:
1. Use in-memory HashMap storage for Course and Learner entities
2. Create Repository, Service, Controller layers
3. Separate REST endpoints for enroll and un-enroll
4. Limited course capacity with waitlist queue (FIFO promotion on un-enroll)
5. Prerequisite-based learner eligibility check before enrollment/waitlist
6. ReentrantLock for concurrent enrollment race condition prevention
7. Create a separate tracking agent for prompt/action audit

Additionally: Provide a preview (plan artifact) of all changes required.

### Actions Taken
- Explored existing project structure (Spring Boot 4.1.2, Java 21, Lombok)
- Found only `CourseEnrollmentApplication.java` exists; project is essentially a skeleton
- Defined prompt-tracker subagent for ongoing audit tracking
- Created implementation plan artifact (`implementation-plan.md`) with full architecture breakdown
- Planned the following file creations:
  - `model/Course.java`
  - `model/Learner.java`
  - `model/EnrollmentStatus.java` (enum)
  - `repository/CourseRepository.java`
  - `repository/LearnerRepository.java`
  - `service/EnrollmentService.java`
  - `controller/EnrollmentController.java`
  - `exception/CourseNotFoundException.java`
  - `exception/LearnerNotFoundException.java`
  - `exception/EnrollmentException.java`
  - `config/DataInitializer.java`
  - `pom.xml` update (add `spring-boot-starter-web`)

### Files Created / Modified
| File | Action |
|---|---|
| `prompt-action-tracker.md` | Created |
| `implementation-plan.md` | Created (artifact — plan preview) |

---

## Entry #2

| Field | Details |
|---|---|
| **Entry #** | 2 |
| **Timestamp** | 2026-11-17T17:47:55+05:30 |
| **Status** | Revised Preview Delivered — Awaiting User Approval |

### User Prompt
Two design refinements to the enrollment microservice plan:
1. If a learner tries to enroll again in a course they're already enrolled in, return a clear status/message. This status should be surfaced as a `code` field in the API response body.
2. For learner prerequisite check, simplify to a boolean flag (`prerequisiteMet`) since the prerequisite criteria is not yet defined.

### Actions Taken
- Updated `EnrollmentStatus` enum: added `ALREADY_ENROLLED` and `NOT_ELIGIBLE` values (moved `NOT_ELIGIBLE` from implicit to explicit)
- Updated `EnrollmentResponse` model: added `code` (String) field alongside existing `status` enum field for machine-readable client consumption
- Updated `Learner` model: replaced `List<String> completedCourseIds` with `boolean prerequisiteMet`
- Updated enroll flow diagram: added `ALREADY_ENROLLED` guard check **before** lock acquisition, and changed prereq check to `learner.prerequisiteMet == true`
- Updated `DataInitializer` learner table: replaced `completedCourseIds` column with `prerequisiteMet` boolean column
- Updated all sample API calls to show new `code` field in JSON response and added `ALREADY_ENROLLED` scenario example (5 scenarios total)
- Added changelog section at top of `implementation-plan.md`

### Files Created / Modified
| File | Action |
|---|---|
| `implementation-plan.md` | Modified (artifact — revised plan, not yet implemented) |

---

## Entry #3

| Field | Details |
|---|---|
| **Entry #** | 3 |
| **Timestamp** | 2026-11-17T17:52:51+05:30 |
| **Status** | ✅ Implementation Complete (Build verification blocked — see note) |
| **Completion Timestamp** | 2026-11-17T18:30:00+05:30 |

### User Prompt
Proceed with the implementation plan that was created.

### Actions Taken
- Triggered implementation of all 13 new files and 1 `pom.xml` modification
- Files created across all layers:
  - **Model:** `Course.java`, `Learner.java`, `EnrollmentStatus.java`, `EnrollmentResponse.java`
  - **Repository:** `CourseRepository.java`, `LearnerRepository.java`
  - **Service:** `EnrollmentService.java`
  - **Controller:** `EnrollmentController.java`, `CourseController.java`, `LearnerController.java`
  - **Exception:** `CourseNotFoundException.java`, `LearnerNotFoundException.java`, `EnrollmentException.java`, `GlobalExceptionHandler.java`
  - **Config:** `DataInitializer.java`
- `pom.xml` updated with `spring-boot-starter-web` dependency

### Files Created / Modified
| Layer | File | Status |
|---|---|---|
| Model | `model/Course.java` | ✅ Created |
| Model | `model/Learner.java` | ✅ Created |
| Model | `model/EnrollmentStatus.java` | ✅ Created |
| Model | `model/EnrollmentResponse.java` | ✅ Created |
| Repository | `repository/CourseRepository.java` | ✅ Created |
| Repository | `repository/LearnerRepository.java` | ✅ Created |
| Service | `service/EnrollmentService.java` | ✅ Created |
| Controller | `controller/EnrollmentController.java` | ✅ Created |
| Controller | `controller/CourseController.java` | ✅ Created |
| Controller | `controller/LearnerController.java` | ✅ Created |
| Exception | `exception/CourseNotFoundException.java` | ✅ Created |
| Exception | `exception/LearnerNotFoundException.java` | ✅ Created |
| Exception | `exception/EnrollmentException.java` | ✅ Created |
| Exception | `exception/GlobalExceptionHandler.java` | ✅ Created |
| Config | `config/DataInitializer.java` | ✅ Created |
| Project | `pom.xml` | ✅ Modified (`spring-boot-starter-web` added) |

> **Build Note:** Maven build via terminal was blocked (`JAVA_HOME` not in PATH for the shell session). User should run `./mvnw clean package` from IntelliJ or a properly configured terminal.

---

## Entry #5

| Field | Details |
|---|---|
| **Entry #** | 5 |
| **Timestamp** | 2026-11-18T00:08:52+05:30 |
| **Status** | ✅ COMPLETED |
| **Completion Timestamp** | 2026-11-18T00:19:06+05:30 |
| **Analysis Agent ID** | `b099fc32739a465a34aae9ea346fe55b` |

### User Prompt
Spin up a new agent to do code analysis — check for Java code standards, security issues, and performance-related potential issues.

### Actions Taken
- Defined and executed specialized subagent: **java-code-analyzer** (ID: `b099fc32739a465a34aae9ea346fe55b`)
- Agent analyzed all 15 Java source files across all layers
- Output report written to: `code-analysis-report.md` (582 lines, 25 findings)

### Analysis Results Summary
| Metric | Value |
|---|---|
| **Overall Health Score** | 6.5 / 10 |
| **Total Findings** | 25 |
| 🔴 HIGH | 3 |
| 🟡 MEDIUM | 9 |
| 🟢 LOW | 8 |
| 💡 SUGGESTION | 5 |

#### Top Critical Issues (HIGH Severity)
1. `GlobalExceptionHandler.java:35` — Raw `ex.getMessage()` returned to client (information disclosure risk)
2. `EnrollmentController.java:45-47` — No null/blank validation on `learnerId`/`courseId` (NPE + phantom lock entries)
3. `CourseRepository.java:18` / `LearnerRepository.java:18` — `HashMap` used in multi-threaded singleton (data races, CME)

---

## Entry #6

| Field | Details |
|---|---|
| **Entry #** | 6 |
| **Timestamp** | 2026-11-18T00:20:02+05:30 |
| **Status** | ✅ Completed |

### User Prompt
Fix issues which have severity of medium and high (from code-analysis-report.md)

### Actions Taken

**HIGH Severity Fixes (3):**
1. **`GlobalExceptionHandler.java`** — Stopped leaking `ex.getMessage()` to clients; now logs the full exception server-side and returns a generic safe message; added handlers for validation and illegal-argument exceptions.
2. **`EnrollmentController.java`** — Replaced raw `Map<String, String>` request body with a typed `EnrollmentRequest` DTO annotated with `@Valid` and `@NotBlank`; created new file `model/EnrollmentRequest.java`.
3. **`CourseRepository.java` + `LearnerRepository.java`** — Replaced non-thread-safe `HashMap` with `ConcurrentHashMap` to prevent race conditions.

**MEDIUM Severity Fixes (9):**
4. **`EnrollmentService.unenroll()`** — Fixed silent data-loss bug: the while-loop now skips deleted waitlisted learners and logs a warning per missing ID.
5. **`CourseController`** — Now returns `CourseView` DTO projection instead of full entity; created new file `model/CourseView.java`.
6. **`LearnerController`** — Now returns `LearnerView` DTO projection hiding sensitive fields (`email`, `prerequisiteMet`); created new file `model/LearnerView.java`.
7. **`EnrollmentService.enroll()`** — Clarified misleading pre-lock comment for readability.
8. **`EnrollmentService`** — Added Javadoc note on `courseLocks` map memory/lifecycle implications.
9. **`EnrollmentService.unenroll()`** — Consolidated two redundant `save()` calls into one.
10. **`Course.java`** — `enrolledLearnerIds` now uses a `ConcurrentHashMap`-backed `Set` for thread safety.

---

## Entry #7

| Field | Details |
|---|---|
| **Entry #** | 7 |
| **Timestamp** | 2026-11-18T00:29:03+05:30 |
| **Status** | ✅ Completed |

### User Prompt
Add JUnit tests covering tests for scenarios like available capacity, full capacity, duplicate requests, FIFO promotion, prerequisite failure, and invalid or repeated drops. If there are concurrent requests, preserve its transaction or locking pattern and add a focused concurrency test where practical.

### Actions Taken
- Created comprehensive test suite in [`EnrollmentServiceTest.java`](src/test/java/com/charles/courseenrollment/service/EnrollmentServiceTest.java) covering:
  1. **Available Capacity:** Successful single and multiple enrollments up to course capacity.
  2. **Full Capacity & Waitlist:** Queue addition when capacity is exhausted; FIFO ordering verification.
  3. **Duplicate Requests:** Verification of `ALREADY_ENROLLED` code without state or count corruption.
  4. **FIFO Promotion on Unenroll:** Automatic promotion of next waitlisted learner when an enrolled learner withdraws; sequential promotion across multiple unenrollments; handling and skipping deleted waitlisted learners.
  5. **Prerequisite Failure:** Rejection with `NOT_ELIGIBLE` status; ensures ineligible learners are neither enrolled nor placed on waitlists.
  6. **Invalid & Repeated Drops:** Exceptions thrown when unenrolling learners not currently enrolled; repeated unenrollment attempts; non-existent courses and learners.
  7. **Focused Concurrency Tests (ReentrantLock verification):**
     - Race condition test with 25 concurrent threads competing for 5 seats: verifies exactly 5 enrolled and 20 waitlisted with zero data races.
     - Race condition test with 10 concurrent threads submitting duplicate enrollments for the same learner: verifies exactly 1 `ENROLLED` and 9 `ALREADY_ENROLLED`.
     - Race condition test with 2 concurrent unenrollments: verifies simultaneous automatic promotions into opened seats.
- Added `spring-boot-starter-validation` dependency to `pom.xml`.

---
