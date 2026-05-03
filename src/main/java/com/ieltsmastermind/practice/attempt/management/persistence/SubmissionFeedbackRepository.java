package com.ieltsmastermind.practice.attempt.management.persistence;

import com.ieltsmastermind.practice.attempt.management.domain.entity.SubmissionFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionFeedbackRepository extends JpaRepository<SubmissionFeedback, String> {

    List<SubmissionFeedback> findAllBySubmissionIdOrderByCreatedAtAsc(String submissionId);
}
