package com.ieltsmastermind.practice.analytics.management.persistence;

import com.ieltsmastermind.practice.analytics.management.domain.entity.LearnerQuestionTypeAnalytics;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LearnerQuestionTypeAnalyticsRepository
        extends JpaRepository<LearnerQuestionTypeAnalytics, String> {

    List<LearnerQuestionTypeAnalytics> findByLearnerAnalyticsSnapshot_IdAndQuestionTypeIn(
            String snapshotId,
            List<PracticeQuestionType> questionTypes
    );
}