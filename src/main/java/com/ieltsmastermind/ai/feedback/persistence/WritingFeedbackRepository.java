package com.ieltsmastermind.ai.feedback.persistence;

import com.ieltsmastermind.ai.feedback.domain.entity.ReadingFeedback;
import com.ieltsmastermind.ai.feedback.domain.entity.WritingFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface WritingFeedbackRepository
        extends JpaRepository<WritingFeedback, Long> {

    Optional<WritingFeedback> findBySubmissionId(String submissionId);

    @Query("""
SELECT f FROM WritingFeedback f
JOIN FETCH f.questions q
LEFT JOIN FETCH q.evidence
WHERE f.submissionId = :submissionId
""")
    Optional<WritingFeedback> findFullBySubmissionId(String submissionId);
}