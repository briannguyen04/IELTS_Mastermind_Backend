package com.ieltsmastermind.practice.attempt.management.domain.entity;

import com.ieltsmastermind.practice.attempt.management.domain.enums.FeedbackAuthorType;
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
@Table(name = "submission_feedback")
public class SubmissionFeedback {

    @Id
    @Column(name = "submission_feedback_id", nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @Enumerated(EnumType.STRING)
    @Column(name = "author_type", nullable = false)
    private FeedbackAuthorType authorType;

    @Column(name = "feedback_content", nullable = false, columnDefinition = "TEXT")
    private String feedbackContent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_practice_submission_id",
            referencedColumnName = "user_practice_submission_id",
            nullable = false
    )
    private UserPracticeSubmission submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            referencedColumnName = "user_id"
    )
    private User author;

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