package com.ieltsmastermind.practice.attempt.management.persistence;

import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeWritingAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPracticeWritingAnswerRepository
        extends JpaRepository<UserPracticeWritingAnswer, String> {

    List<UserPracticeWritingAnswer> findAllBySubmission_IdOrderByOrderIndexAsc(String submissionId);
}
