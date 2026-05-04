package com.ieltsmastermind.practice.attempt.management.domain.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieltsmastermind.practice.attempt.management.domain.enums.TutorStatus;
import com.ieltsmastermind.practice.content.management.domain.entity.PracticeContent;
import com.ieltsmastermind.practice.studyplan.management.domain.entity.LearnerStudyPlan;
import com.ieltsmastermind.user.management.domain.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_practice_submission")
public class UserPracticeSubmission {

    @Id
    @Column(name = "user_practice_submission_id", nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @Column(name = "user_id")
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tutor_status", nullable = false)
    private TutorStatus tutorStatus = TutorStatus.PENDING;

    @Column(name = "practice_content_id")
    private String practiceContentId;

    @Column(name = "time_spent_seconds", nullable = false)
    private Integer timeSpentSeconds;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "score", nullable = false)
    private Double score = 0.0;

    @Column(name = "correct_answer_percentage", nullable = false)
    private Double correctAnswerPercentage = 0.0;

    @Column(name = "correct_answer_count", nullable = false)
    private Integer correctAnswerCount = 0;

    @Column(name = "wrong_answer_count", nullable = false)
    private Integer wrongAnswerCount = 0;

    @Column(name = "skip_answer_count", nullable = false)
    private Integer skipAnswerCount = 0;

    @Column(name = "total_question_count", nullable = false)
    private Integer totalQuestionCount = 0;

    @Column(name = "answered_question_count", nullable = false)
    private Integer answeredQuestionCount = 0;

    @Column(name = "accuracy_rate", nullable = false)
    private Double accuracyRate = 0.0;

    @Column(name = "skip_rate", nullable = false)
    private Double skipRate = 0.0;

    @Column(name = "effective_accuracy", nullable = false)
    private Double effectiveAccuracy = 0.0;

    @Column(name = "is_tutor_review_requested", nullable = false)
    private boolean isTutorReviewRequested = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "learner_test_activities", columnDefinition = "JSON")
    private JsonNode learnerTestActivities;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practice_content_id", referencedColumnName = "practice_content_id",
            insertable = false, updatable = false)
    private PracticeContent practiceContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            referencedColumnName = "user_id",
            insertable = false,
            updatable = false)
    private User user;

    @OneToMany(mappedBy = "submission")
    @OrderBy("orderIndex ASC")
    private List<UserPracticeSubmissionAnswer> answerRows = new ArrayList<>();

    @OneToMany(mappedBy = "submission")
    @OrderBy("orderIndex ASC")
    private List<UserPracticeWritingAnswer> writingAnswers = new ArrayList<>();

    @OneToMany(mappedBy = "submission")
    @OrderBy("createdAt ASC")
    private List<SubmissionFeedback> submissionFeedbacks = new ArrayList<>();

    @OneToMany(mappedBy = "userPracticeSubmission")
    private List<TutorUserPracticeSubmission> tutorUserPracticeSubmissions = new ArrayList<>();

    @OneToMany(mappedBy = "submission")
    @OrderBy("questionType ASC")
    private List<SubmissionQuestionTypeAccuracy> questionTypeAccuracies = new ArrayList<>();

    @OneToMany(mappedBy = "submission")
    @OrderBy("topicTag ASC")
    private List<SubmissionTopicTagAccuracy> topicTagAccuracies = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.submittedAt = LocalDateTime.now();
    }
}