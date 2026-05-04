package com.ieltsmastermind.practice.studyplan.management.domain.entity;

import com.ieltsmastermind.practice.analytics.management.domain.entity.*;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeSubmission;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanStatus;
import com.ieltsmastermind.user.management.domain.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "learner_study_plan",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_study_plan_user_skill_version",
                        columnNames = {
                                "user_id",
                                "skill",
                                "version_number"
                        }
                )
        }
)
public class LearnerStudyPlan {

    @Id
    @Column(name = "learner_study_plan_id", nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @Enumerated(EnumType.STRING)
    @Column(name = "skill", nullable = false)
    private PracticeContentSkill skill;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LearnerStudyPlanStatus status = LearnerStudyPlanStatus.INACTIVE;

    @Column(name = "ready_to_finalize", nullable = false)
    private boolean readyToFinalize = false;

    @Column(name = "submission_count_used", nullable = false)
    private Integer submissionCountUsed = 0;

    @Column(name = "submission_count_since_creation", nullable = false)
    private Integer submissionCountSinceCreation = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            referencedColumnName = "user_id"
    )
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "submission_analytics_id",
            referencedColumnName = "submission_analytics_id"
    )
    private SubmissionAnalytics submissionAnalytics;

    @OneToMany(mappedBy = "learnerStudyPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("priorityRank ASC")
    private List<LearnerStudyPlanTask> tasks = new ArrayList<>();

    @OneToMany(mappedBy = "learnerStudyPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("strengthRank ASC")
    private List<LearnerStudyPlanStrengthBlock> strengthBlocks = new ArrayList<>();

    @OneToMany(mappedBy = "learnerStudyPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("weaknessRank ASC")
    private List<LearnerStudyPlanWeaknessBlock> weaknessBlocks = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
