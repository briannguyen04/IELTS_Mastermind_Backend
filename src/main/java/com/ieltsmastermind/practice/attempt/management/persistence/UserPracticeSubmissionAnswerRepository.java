package com.ieltsmastermind.practice.attempt.management.persistence;

import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeSubmission;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeSubmissionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserPracticeSubmissionAnswerRepository extends JpaRepository<UserPracticeSubmissionAnswer, String> {

    List<UserPracticeSubmissionAnswer> findAllBySubmission_IdOrderByOrderIndexAsc(String submissionId);
    List<UserPracticeSubmissionAnswer> findBySubmissionId(String submissionId);
}
