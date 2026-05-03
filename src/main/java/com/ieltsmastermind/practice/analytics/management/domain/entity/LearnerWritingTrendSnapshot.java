package com.ieltsmastermind.practice.analytics.management.domain.entity;

import com.ieltsmastermind.practice.analytics.management.domain.enums.TrendLabel;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeSubmission;
import com.ieltsmastermind.practice.studyplan.management.domain.entity.LearnerStudyPlan;
import com.ieltsmastermind.user.management.domain.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "learner_writing_trend_snapshot",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_writing_trend_snapshot_user_version",
                        columnNames = {
                                "user_id",
                                "version_number"
                        }
                )
        }
)
public class LearnerWritingTrendSnapshot {

    @Id
    @Column(name = "learner_writing_trend_snapshot_id", nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber = 1;

    @Column(name = "submission_count_used", nullable = false)
    private Integer submissionCountUsed = 0;

    @Column(name = "overall_band_score_data_point_count", nullable = false)
    private Integer overallBandScoreDataPointCount = 0;

    @Column(name = "overall_band_score_x_mean", nullable = false)
    private Double overallBandScoreXMean = 0.0;

    @Column(name = "overall_band_score_y_mean", nullable = false)
    private Double overallBandScoreYMean = 0.0;

    @Column(name = "overall_band_score_numerator", nullable = false)
    private Double overallBandScoreNumerator = 0.0;

    @Column(name = "overall_band_score_denominator", nullable = false)
    private Double overallBandScoreDenominator = 0.0;

    @Column(name = "overall_band_score_slope", nullable = false)
    private Double overallBandScoreSlope = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_band_score_trend_label", nullable = false)
    private TrendLabel overallBandScoreTrendLabel = TrendLabel.STABLE;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            referencedColumnName = "user_id",
            nullable = false
    )
    private User user;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "learner_writing_trend_snapshot_submission",
            joinColumns = @JoinColumn(
                    name = "learner_writing_trend_snapshot_id",
                    referencedColumnName = "learner_writing_trend_snapshot_id"
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "user_practice_submission_id",
                    referencedColumnName = "user_practice_submission_id"
            )
    )
    private Set<UserPracticeSubmission> submissions = new LinkedHashSet<>();

    @OneToMany(mappedBy = "learnerWritingTrendSnapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionType ASC")
    private Set<LearnerWritingQuestionTypeTrend> questionTypeTrends = new LinkedHashSet<>();

    @OneToMany(mappedBy = "learnerWritingTrendSnapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("topicTag ASC")
    private Set<LearnerWritingTopicTagTrend> topicTagTrends = new LinkedHashSet<>();

    @OneToOne(mappedBy = "learnerWritingTrendSnapshot", fetch = FetchType.LAZY)
    private LearnerStudyPlan learnerStudyPlan;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.calculatedAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
