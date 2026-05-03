package com.ieltsmastermind.practice.attempt.management.domain.entity;

import com.ieltsmastermind.user.management.domain.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "user_practice_writing_review",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_writing_review_answer_reviewer",
                        columnNames = {"user_practice_writing_answer_id", "reviewed_by_user_id"}
                )
        }
)
public class UserPracticeWritingReview {

    @Id
    @Column(name = "user_practice_writing_review_id", nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @Column(name = "overall_tutor_band", nullable = false)
    private Double overallTutorBand = 0.0;

    @Column(name = "tutor_task_achievement_band", nullable = false)
    private Double tutorTaskAchievementBand = 0.0;

    @Column(name = "tutor_task_response_band", nullable = false)
    private Double tutorTaskResponseBand = 0.0;

    @Column(name = "tutor_coherence_and_cohesion_band", nullable = false)
    private Double tutorCoherenceAndCohesionBand = 0.0;

    @Column(name = "tutor_lexical_resource_band", nullable = false)
    private Double tutorLexicalResourceBand = 0.0;

    @Column(name = "tutor_grammatical_range_and_accuracy_band", nullable = false)
    private Double tutorGrammaticalRangeAndAccuracyBand = 0.0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_practice_writing_answer_id", nullable = false, updatable = false)
    private UserPracticeWritingAnswer writingAnswer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id", referencedColumnName = "user_id", nullable = false, updatable = false)
    private User reviewedByUser;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
