package com.ieltsmastermind.practice.analytics.management.persistence;

import com.ieltsmastermind.practice.analytics.management.domain.entity.LearnerTopicTagAnalytics;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTopicTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LearnerTopicTagAnalyticsRepository
        extends JpaRepository<LearnerTopicTagAnalytics, String> {

    List<LearnerTopicTagAnalytics> findByLearnerAnalyticsSnapshot_IdAndTopicTagIn(
            String snapshotId,
            List<PracticeTopicTag> topicTags
    );
}