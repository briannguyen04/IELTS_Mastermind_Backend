package com.ieltsmastermind.practice.analytics.management.domain.entity;

import com.ieltsmastermind.practice.analytics.management.domain.enums.TrendLabel;
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
        name = "learner_topic_tag_trend",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_trend_snapshot_topic_tag",
                        columnNames = {
                                "learner_trend_snapshot_id",
                                "topic_tag"
                        }
                )
        }
)
public class LearnerTopicTagTrend {

    @Id
    @Column(name = "learner_topic_tag_trend_id", nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @Enumerated(EnumType.STRING)
    @Column(name = "topic_tag", nullable = false)
    private PracticeTopicTag topicTag;

    @Column(name = "contributed_submission_count", nullable = false)
    private Integer contributedSubmissionCount = 0;

    @Column(name = "accuracy_data_point_count", nullable = false)
    private Integer accuracyDataPointCount = 0;

    @Column(name = "accuracy_x_mean", nullable = false)
    private Double accuracyXMean = 0.0;

    @Column(name = "accuracy_y_mean", nullable = false)
    private Double accuracyYMean = 0.0;

    @Column(name = "accuracy_numerator", nullable = false)
    private Double accuracyNumerator = 0.0;

    @Column(name = "accuracy_denominator", nullable = false)
    private Double accuracyDenominator = 0.0;

    @Column(name = "accuracy_slope", nullable = false)
    private Double accuracySlope = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "accuracy_trend_label", nullable = false)
    private TrendLabel accuracyTrendLabel = TrendLabel.STABLE;

    @Column(name = "skip_rate_data_point_count", nullable = false)
    private Integer skipRateDataPointCount = 0;

    @Column(name = "skip_rate_x_mean", nullable = false)
    private Double skipRateXMean = 0.0;

    @Column(name = "skip_rate_y_mean", nullable = false)
    private Double skipRateYMean = 0.0;

    @Column(name = "skip_rate_numerator", nullable = false)
    private Double skipRateNumerator = 0.0;

    @Column(name = "skip_rate_denominator", nullable = false)
    private Double skipRateDenominator = 0.0;

    @Column(name = "skip_rate_slope", nullable = false)
    private Double skipRateSlope = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "skip_rate_trend_label", nullable = false)
    private TrendLabel skipRateTrendLabel = TrendLabel.STABLE;

    @Column(name = "effective_accuracy_data_point_count", nullable = false)
    private Integer effectiveAccuracyDataPointCount = 0;

    @Column(name = "effective_accuracy_x_mean", nullable = false)
    private Double effectiveAccuracyXMean = 0.0;

    @Column(name = "effective_accuracy_y_mean", nullable = false)
    private Double effectiveAccuracyYMean = 0.0;

    @Column(name = "effective_accuracy_numerator", nullable = false)
    private Double effectiveAccuracyNumerator = 0.0;

    @Column(name = "effective_accuracy_denominator", nullable = false)
    private Double effectiveAccuracyDenominator = 0.0;

    @Column(name = "effective_accuracy_slope", nullable = false)
    private Double effectiveAccuracySlope = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "effective_accuracy_trend_label", nullable = false)
    private TrendLabel effectiveAccuracyTrendLabel = TrendLabel.STABLE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "learner_trend_snapshot_id",
            referencedColumnName = "learner_trend_snapshot_id",
            nullable = false
    )
    private LearnerTrendSnapshot learnerTrendSnapshot;
}
