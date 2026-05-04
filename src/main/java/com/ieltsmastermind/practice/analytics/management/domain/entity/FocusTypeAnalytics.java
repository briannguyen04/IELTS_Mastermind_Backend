package com.ieltsmastermind.practice.analytics.management.domain.entity;

import com.ieltsmastermind.practice.analytics.management.domain.enums.AnalyticsStrengthLabel;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTopicTag;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanFocusType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "focus_type_analytics")
public class FocusTypeAnalytics {

    @Id
    @Column(name = "focus_type_analytics_id", nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @Enumerated(EnumType.STRING)
    @Column(name = "focus_type", nullable = false)
    private LearnerStudyPlanFocusType focusType;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type")
    private PracticeQuestionType questionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "topic_tag")
    private PracticeTopicTag topicTag;

    @Column(name = "contributed_submission_count", nullable = false)
    private Integer contributedSubmissionCount = 0;

    @Column(name = "rolling_exposure_count", nullable = false)
    private Integer rollingExposureCount = 0;

    @Column(name = "rolling_correct_answer_percentage", nullable = false)
    private Double rollingCorrectAnswerPercentage = 0.0;

    @Column(name = "rolling_overall_band_score", nullable = false)
    private Double rollingOverallBandScore = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "strength_label", nullable = false)
    private AnalyticsStrengthLabel strengthLabel = AnalyticsStrengthLabel.NEUTRAL;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "submission_analytics_id",
            referencedColumnName = "submission_analytics_id",
            nullable = false
    )
    private SubmissionAnalytics submissionAnalytics;
}