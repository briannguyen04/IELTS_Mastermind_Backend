package com.ieltsmastermind.practice.attempt.management.domain.entity;

import com.ieltsmastermind.practice.attempt.management.domain.enums.FeedbackAuthorType;
import com.ieltsmastermind.practice.attempt.management.domain.enums.WritingCriterionName;
import com.ieltsmastermind.practice.attempt.management.domain.enums.WritingFeedbackLabel;
import com.ieltsmastermind.practice.attempt.management.domain.enums.WritingFeedbackType;
import com.ieltsmastermind.user.management.domain.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_practice_writing_criterion_feedback")
public class UserPracticeWritingCriterionFeedback {

    @Id
    @Column(name = "user_practice_writing_criterion_feedback_id", nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @Enumerated(EnumType.STRING)
    @Column(name = "author_type", nullable = false)
    private FeedbackAuthorType authorType;

    @Enumerated(EnumType.STRING)
    @Column(name = "criterion_name", nullable = false)
    private WritingCriterionName criterionName;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", nullable = false)
    private WritingFeedbackType feedbackType;

    @Enumerated(EnumType.STRING)
    @Column(name = "label", nullable = false)
    private WritingFeedbackLabel label;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_sentences", columnDefinition = "JSON")
    private List<String> evidenceSentences  = new ArrayList<>();

    @Column(name = "recommended_action_description", columnDefinition = "TEXT")
    private String recommendedActionDescription;

    @Column(name = "recommended_action_explanation", columnDefinition = "TEXT")
    private String recommendedActionExplanation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_practice_writing_answer_id", nullable = false)
    private UserPracticeWritingAnswer writingAnswer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id", referencedColumnName = "user_id")
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
