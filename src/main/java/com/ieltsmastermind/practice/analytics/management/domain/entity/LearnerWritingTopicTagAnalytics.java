package com.ieltsmastermind.practice.analytics.management.domain.entity;

import com.ieltsmastermind.practice.analytics.management.domain.enums.AnalyticsStrengthLabel;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTopicTag;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "learner_writing_topic_tag_analytics",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_writing_snapshot_topic_tag",
                        columnNames = {
                                "learner_writing_analytics_snapshot_id",
                                "topic_tag"
                        }
                )
        }
)
public class LearnerWritingTopicTagAnalytics {

    @Id
    @Column(name = "learner_writing_topic_tag_analytics_id", nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @Enumerated(EnumType.STRING)
    @Column(name = "topic_tag", nullable = false)
    private PracticeTopicTag topicTag;

    @Column(name = "contributed_submission_count", nullable = false)
    private Integer contributedSubmissionCount = 0;

    @Column(name = "rolling_overall_band_score", nullable = false)
    private Double rollingOverallBandScore = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "strength_label", nullable = false)
    private AnalyticsStrengthLabel strengthLabel = AnalyticsStrengthLabel.NEUTRAL;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "learner_writing_analytics_snapshot_id",
            referencedColumnName = "learner_writing_analytics_snapshot_id",
            nullable = false
    )
    private LearnerWritingAnalyticsSnapshot learnerWritingAnalyticsSnapshot;
}
