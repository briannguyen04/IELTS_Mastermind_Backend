package com.ieltsmastermind.practice.attempt.management.persistence;

import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeWritingCriterionFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPracticeWritingCriterionFeedbackRepository
        extends JpaRepository<UserPracticeWritingCriterionFeedback, String> {

    @Query("""
        SELECT f
        FROM UserPracticeWritingCriterionFeedback f
        WHERE f.writingAnswer.submission.id = :submissionId
        ORDER BY f.createdAt ASC
    """)
    List<UserPracticeWritingCriterionFeedback> findAllBySubmissionIdOrderByCreatedAtAsc(
            @Param("submissionId") String submissionId
    );
}
