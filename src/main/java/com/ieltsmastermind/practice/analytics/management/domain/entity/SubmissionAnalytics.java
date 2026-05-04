package com.ieltsmastermind.practice.analytics.management.domain.entity;

import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeSubmission;
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
        name = "submission_analytics",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_submission_analytics_user_skill_version",
                        columnNames = {
                                "user_id",
                                "skill",
                                "version_number"
                        }
                )
        }
)
public class SubmissionAnalytics {

    @Id
    @Column(name = "submission_analytics_id", nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @Enumerated(EnumType.STRING)
    @Column(name = "skill", nullable = false)
    private PracticeContentSkill skill;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber = 1;

    @Column(name = "submission_count_used", nullable = false)
    private Integer submissionCountUsed = 0;

    @Column(name = "rolling_correct_answer_percentage", nullable = false)
    private Double rollingCorrectAnswerPercentage = 0.0;

    @Column(name = "rolling_overall_band_score", nullable = false)
    private Double rollingOverallBandScore = 0.0;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "submissionAnalytics", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("focusType ASC")
    private Set<FocusTypeAnalytics> focusTypeAnalytics = new LinkedHashSet<>();

    @OneToOne(mappedBy = "submissionAnalytics", fetch = FetchType.LAZY)
    private LearnerStudyPlan learnerStudyPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            referencedColumnName = "user_id"
    )
    private User user;

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