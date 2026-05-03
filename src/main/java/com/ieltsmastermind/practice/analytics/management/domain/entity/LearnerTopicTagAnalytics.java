package com.ieltsmastermind.practice.analytics.management.domain.entity;

import com.ieltsmastermind.practice.analytics.management.domain.enums.AnalyticsConclusionLabel;
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
        name = "learner_topic_tag_analytics",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_snapshot_topic_tag",
                        columnNames = {
                                "learner_analytics_snapshot_id",
                                "topic_tag"
                        }
                )
        }
)
public class LearnerTopicTagAnalytics {

    @Id
    @Column(name = "learner_topic_tag_analytics_id", nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @Enumerated(EnumType.STRING)
    @Column(name = "topic_tag", nullable = false)
    private PracticeTopicTag topicTag;

    @Column(name = "contributed_submission_count", nullable = false)
    private Integer contributedSubmissionCount = 0;

    @Column(name = "rolling_exposure_count", nullable = false)
    private Integer rollingExposureCount = 0;

    @Column(name = "rolling_answered_question_count", nullable = false)
    private Integer rollingAnsweredQuestionCount = 0;

    @Column(name = "rolling_correct_question_count", nullable = false)
    private Integer rollingCorrectQuestionCount = 0;

    @Column(name = "rolling_wrong_question_count", nullable = false)
    private Integer rollingWrongQuestionCount = 0;

    @Column(name = "rolling_skip_question_count", nullable = false)
    private Integer rollingSkipQuestionCount = 0;

    @Column(name = "rolling_correct_answer_percentage", nullable = false)
    private Double rollingCorrectAnswerPercentage = 0.0;

    @Column(name = "rolling_skip_rate", nullable = false)
    private Double rollingSkipRate = 0.0;

    @Column(name = "rolling_effective_accuracy", nullable = false)
    private Double rollingEffectiveAccuracy = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "strength_label", nullable = false)
    private AnalyticsStrengthLabel strengthLabel = AnalyticsStrengthLabel.NEUTRAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "conclusion_label", nullable = false)
    private AnalyticsConclusionLabel conclusionLabel = AnalyticsConclusionLabel.BALANCED;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "learner_analytics_snapshot_id",
            referencedColumnName = "learner_analytics_snapshot_id",
            nullable = false
    )
    private LearnerAnalyticsSnapshot learnerAnalyticsSnapshot;
}
