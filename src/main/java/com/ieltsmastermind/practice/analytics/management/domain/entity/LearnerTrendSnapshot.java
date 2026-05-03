package com.ieltsmastermind.practice.analytics.management.domain.entity;

import com.ieltsmastermind.practice.analytics.management.domain.enums.TrendLabel;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;
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
        name = "learner_trend_snapshot",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_trend_snapshot_user_skill_version",
                        columnNames = {
                                "user_id",
                                "skill",
                                "version_number"
                        }
                )
        }
)
public class LearnerTrendSnapshot {

    @Id
    @Column(name = "learner_trend_snapshot_id", nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @Enumerated(EnumType.STRING)
    @Column(name = "skill", nullable = false)
    private PracticeContentSkill skill;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber = 1;

    @Column(name = "submission_count_used", nullable = false)
    private Integer submissionCountUsed = 0;

    @Column(name = "overall_accuracy_data_point_count", nullable = false)
    private Integer overallAccuracyDataPointCount = 0;

    @Column(name = "overall_accuracy_x_mean", nullable = false)
    private Double overallAccuracyXMean = 0.0;

    @Column(name = "overall_accuracy_y_mean", nullable = false)
    private Double overallAccuracyYMean = 0.0;

    @Column(name = "overall_accuracy_numerator", nullable = false)
    private Double overallAccuracyNumerator = 0.0;

    @Column(name = "overall_accuracy_denominator", nullable = false)
    private Double overallAccuracyDenominator = 0.0;

    @Column(name = "overall_accuracy_slope", nullable = false)
    private Double overallAccuracySlope = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_accuracy_trend_label", nullable = false)
    private TrendLabel overallAccuracyTrendLabel = TrendLabel.STABLE;

    @Column(name = "overall_skip_rate_data_point_count", nullable = false)
    private Integer overallSkipRateDataPointCount = 0;

    @Column(name = "overall_skip_rate_x_mean", nullable = false)
    private Double overallSkipRateXMean = 0.0;

    @Column(name = "overall_skip_rate_y_mean", nullable = false)
    private Double overallSkipRateYMean = 0.0;

    @Column(name = "overall_skip_rate_numerator", nullable = false)
    private Double overallSkipRateNumerator = 0.0;

    @Column(name = "overall_skip_rate_denominator", nullable = false)
    private Double overallSkipRateDenominator = 0.0;

    @Column(name = "overall_skip_rate_slope", nullable = false)
    private Double overallSkipRateSlope = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_skip_rate_trend_label", nullable = false)
    private TrendLabel overallSkipRateTrendLabel = TrendLabel.STABLE;

    @Column(name = "overall_effective_accuracy_data_point_count", nullable = false)
    private Integer overallEffectiveAccuracyDataPointCount = 0;

    @Column(name = "overall_effective_accuracy_x_mean", nullable = false)
    private Double overallEffectiveAccuracyXMean = 0.0;

    @Column(name = "overall_effective_accuracy_y_mean", nullable = false)
    private Double overallEffectiveAccuracyYMean = 0.0;

    @Column(name = "overall_effective_accuracy_numerator", nullable = false)
    private Double overallEffectiveAccuracyNumerator = 0.0;

    @Column(name = "overall_effective_accuracy_denominator", nullable = false)
    private Double overallEffectiveAccuracyDenominator = 0.0;

    @Column(name = "overall_effective_accuracy_slope", nullable = false)
    private Double overallEffectiveAccuracySlope = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_effective_accuracy_trend_label", nullable = false)
    private TrendLabel overallEffectiveAccuracyTrendLabel = TrendLabel.STABLE;

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

    @OneToMany(mappedBy = "learnerTrendSnapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("submissionIndex ASC")
    private Set<LearnerTrendSnapshotSubmission> submissions = new LinkedHashSet<>();

    @OneToMany(mappedBy = "learnerTrendSnapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionType ASC")
    private Set<LearnerQuestionTypeTrend> questionTypeTrends = new LinkedHashSet<>();

    @OneToMany(mappedBy = "learnerTrendSnapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("topicTag ASC")
    private Set<LearnerTopicTagTrend> topicTagTrends = new LinkedHashSet<>();

    @OneToOne(mappedBy = "learnerTrendSnapshot", fetch = FetchType.LAZY)
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
