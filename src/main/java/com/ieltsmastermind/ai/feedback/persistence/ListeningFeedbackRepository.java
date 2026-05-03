package com.ieltsmastermind.ai.feedback.persistence;

import com.ieltsmastermind.ai.feedback.domain.entity.ListeningFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ListeningFeedbackRepository
        extends JpaRepository<ListeningFeedback, Long> {

    Optional<ListeningFeedback> findBySubmissionId(String submissionId);
    @Query("""
SELECT f FROM ListeningFeedback f
JOIN FETCH f.questions q
LEFT JOIN FETCH q.evidence
WHERE f.submissionId = :submissionId
""")
    Optional<ListeningFeedback> findFullBySubmissionId(String submissionId);
}
