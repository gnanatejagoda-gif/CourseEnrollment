package com.charles.courseenrollment.repository;

import com.charles.courseenrollment.model.Course;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory repository for {@link Course} entities backed by a {@link ConcurrentHashMap}.
 *
 * <p>Uses {@link ConcurrentHashMap} instead of {@link java.util.HashMap} to prevent
 * data races, lost updates, and {@link java.util.ConcurrentModificationException}
 * under concurrent HTTP request threads.</p>
 */
@Repository
public class CourseRepository {

    private final Map<String, Course> courseStore = new ConcurrentHashMap<>();

    /**
     * Persist or overwrite a course.
     *
     * @param course the course to store
     * @return the stored course
     */
    public Course save(Course course) {
        courseStore.put(course.getCourseId(), course);
        return course;
    }

    /**
     * Look up a course by its ID.
     *
     * @param courseId the ID to look up
     * @return an {@link Optional} containing the course, or empty if not found
     */
    public Optional<Course> findById(String courseId) {
        return Optional.ofNullable(courseStore.get(courseId));
    }

    /**
     * Return all stored courses.
     *
     * @return list of all courses
     */
    public List<Course> findAll() {
        return new ArrayList<>(courseStore.values());
    }

    /**
     * Remove a course by its ID.
     *
     * @param courseId the ID of the course to remove
     */
    public void deleteById(String courseId) {
        courseStore.remove(courseId);
    }

    /**
     * Check whether a course with the given ID exists.
     *
     * @param courseId the ID to check
     * @return true if present
     */
    public boolean existsById(String courseId) {
        return courseStore.containsKey(courseId);
    }
}
