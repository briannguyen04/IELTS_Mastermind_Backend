package com.ieltsmastermind.practice.attempt.management.domain.entity;

import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
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
        name = "submission_question_type_accuracy",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_submission_question_type",
                        columnNames = {"user_practice_submission_id", "question_type"}
                )
        }
)
public class SubmissionQuestionTypeAccuracy {

    @Id
    @Column(name = "submission_question_type_accuracy_id", nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private PracticeQuestionType questionType;

    @Column(name = "exposure_count", nullable = false)
    private Integer exposureCount = 0;

    @Column(name = "answered_question_count", nullable = false)
    private Integer answeredQuestionCount = 0;

    @Column(name = "correct_question_count", nullable = false)
    private Integer correctQuestionCount = 0;

    @Column(name = "wrong_question_count", nullable = false)
    private Integer wrongQuestionCount = 0;

    @Column(name = "skip_question_count", nullable = false)
    private Integer skipQuestionCount = 0;

    @Column(name = "correct_answer_percentage", nullable = false)
    private Double correctAnswerPercentage = 0.0;

    @Column(name = "skip_rate", nullable = false)
    private Double skipRate = 0.0;

    @Column(name = "effective_accuracy", nullable = false)
    private Double effectiveAccuracy = 0.0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_practice_submission_id",
            referencedColumnName = "user_practice_submission_id",
            nullable = false
    )
    private UserPracticeSubmission submission;
}
