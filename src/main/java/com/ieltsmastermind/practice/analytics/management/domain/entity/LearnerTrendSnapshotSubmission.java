package com.ieltsmastermind.practice.analytics.management.domain.entity;

import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeSubmission;
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
        name = "learner_trend_snapshot_submission",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_trend_snapshot_submission",
                        columnNames = {
                                "learner_trend_snapshot_id",
                                "user_practice_submission_id"
                        }
                ),
                @UniqueConstraint(
                        name = "uk_trend_snapshot_submission_index",
                        columnNames = {
                                "learner_trend_snapshot_id",
                                "submission_index"
                        }
                )
        }
)
public class LearnerTrendSnapshotSubmission {

    @Id
    @Column(name = "learner_trend_snapshot_submission_id", nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @Column(name = "submission_index", nullable = false)
    private Integer submissionIndex = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "learner_trend_snapshot_id",
            referencedColumnName = "learner_trend_snapshot_id",
            nullable = false
    )
    private LearnerTrendSnapshot learnerTrendSnapshot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_practice_submission_id",
            referencedColumnName = "user_practice_submission_id",
            nullable = false
    )
    private UserPracticeSubmission submission;
}
