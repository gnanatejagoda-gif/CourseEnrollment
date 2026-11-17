package com.charles.courseenrollment.repository;

import com.charles.courseenrollment.model.Learner;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory repository for {@link Learner} entities backed by a {@link ConcurrentHashMap}.
 *
 * <p>Uses {@link ConcurrentHashMap} instead of {@link java.util.HashMap} to prevent
 * data races, lost updates, and {@link java.util.ConcurrentModificationException}
 * under concurrent HTTP request threads.</p>
 */
@Repository
public class LearnerRepository {

    private final Map<String, Learner> learnerStore = new ConcurrentHashMap<>();

    /**
     * Persist or overwrite a learner.
     *
     * @param learner the learner to store
     * @return the stored learner
     */
    public Learner save(Learner learner) {
        learnerStore.put(learner.getLearnerId(), learner);
        return learner;
    }

    /**
     * Look up a learner by their ID.
     *
     * @param learnerId the ID to look up
     * @return an {@link Optional} containing the learner, or empty if not found
     */
    public Optional<Learner> findById(String learnerId) {
        return Optional.ofNullable(learnerStore.get(learnerId));
    }

    /**
     * Return all stored learners.
     *
     * @return list of all learners
     */
    public List<Learner> findAll() {
        return new ArrayList<>(learnerStore.values());
    }

    /**
     * Check whether a learner with the given ID exists.
     *
     * @param learnerId the ID to check
     * @return true if present
     */
    public boolean existsById(String learnerId) {
        return learnerStore.containsKey(learnerId);
    }
}
