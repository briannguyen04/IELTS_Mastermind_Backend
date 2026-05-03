package com.ieltsmastermind.ai.feedback.persistence;

import com.ieltsmastermind.ai.feedback.domain.entity.ReadingFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ReadingFeedbackRepository
        extends JpaRepository<ReadingFeedback, Long> {

    Optional<ReadingFeedback> findBySubmissionId(String submissionId);

    @Query("""
SELECT f FROM ReadingFeedback f
JOIN FETCH f.questions q
LEFT JOIN FETCH q.evidence
WHERE f.submissionId = :submissionId
""")
    Optional<ReadingFeedback> findFullBySubmissionId(String submissionId);
}