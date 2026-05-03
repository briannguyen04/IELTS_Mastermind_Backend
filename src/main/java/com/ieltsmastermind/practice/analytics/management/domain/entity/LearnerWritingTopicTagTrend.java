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
        name = "learner_writing_topic_tag_trend",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_writing_trend_snapshot_topic_tag",
                        columnNames = {
                                "learner_writing_trend_snapshot_id",
                                "topic_tag"
                        }
                )
        }
)
public class LearnerWritingTopicTagTrend {

    @Id
    @Column(name = "learner_writing_topic_tag_trend_id", nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @Enumerated(EnumType.STRING)
    @Column(name = "topic_tag", nullable = false)
    private PracticeTopicTag topicTag;

    @Column(name = "contributed_submission_count", nullable = false)
    private Integer contributedSubmissionCount = 0;

    @Column(name = "band_score_data_point_count", nullable = false)
    private Integer bandScoreDataPointCount = 0;

    @Column(name = "band_score_x_mean", nullable = false)
    private Double bandScoreXMean = 0.0;

    @Column(name = "band_score_y_mean", nullable = false)
    private Double bandScoreYMean = 0.0;

    @Column(name = "band_score_numerator", nullable = false)
    private Double bandScoreNumerator = 0.0;

    @Column(name = "band_score_denominator", nullable = false)
    private Double bandScoreDenominator = 0.0;

    @Column(name = "band_score_slope", nullable = false)
    private Double bandScoreSlope = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "band_score_trend_label", nullable = false)
    private TrendLabel bandScoreTrendLabel = TrendLabel.STABLE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "learner_writing_trend_snapshot_id",
            referencedColumnName = "learner_writing_trend_snapshot_id",
            nullable = false
    )
    private LearnerWritingTrendSnapshot learnerWritingTrendSnapshot;
}
