package com.ieltsmastermind.practice.attempt.management.persistence;

import com.ieltsmastermind.practice.attempt.management.domain.entity.SubmissionTopicTagAccuracy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionTopicTagAccuracyRepository extends JpaRepository<SubmissionTopicTagAccuracy, String> {

    void deleteBySubmission_Id(String submissionId);
}