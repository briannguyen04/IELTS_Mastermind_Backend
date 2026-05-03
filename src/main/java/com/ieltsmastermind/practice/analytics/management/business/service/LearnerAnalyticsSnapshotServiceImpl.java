package com.ieltsmastermind.practice.analytics.management.business.service;

import com.ieltsmastermind.practice.analytics.management.business.interfaces.LearnerAnalyticsSnapshotService;
import com.ieltsmastermind.practice.analytics.management.domain.dto.LearnerAnalyticsSnapshotCreateRequestDto;
import com.ieltsmastermind.practice.analytics.management.domain.dto.LearnerAnalyticsSnapshotResponseDto;
import com.ieltsmastermind.practice.analytics.management.domain.entity.LearnerAnalyticsSnapshot;
import com.ieltsmastermind.practice.analytics.management.domain.entity.LearnerQuestionTypeAnalytics;
import com.ieltsmastermind.practice.analytics.management.domain.entity.LearnerTopicTagAnalytics;
import com.ieltsmastermind.practice.analytics.management.domain.enums.AnalyticsConclusionLabel;
import com.ieltsmastermind.practice.analytics.management.domain.enums.AnalyticsStrengthLabel;
import com.ieltsmastermind.practice.analytics.management.persistence.LearnerAnalyticsSnapshotRepository;
import com.ieltsmastermind.practice.attempt.management.domain.entity.SubmissionQuestionTypeAccuracy;
import com.ieltsmastermind.practice.attempt.management.domain.entity.SubmissionTopicTagAccuracy;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeSubmission;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeSubmissionRepository;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTopicTag;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanFocusType;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanTargetMetric;
import com.ieltsmastermind.user.management.domain.entity.User;
import com.ieltsmastermind.user.management.persistence.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LearnerAnalyticsSnapshotServiceImpl implements LearnerAnalyticsSnapshotService {

    private static final double WEAK_STRENGTH_PERCENTAGE_MAX = 55.0;
    private static final double STRONG_STRENGTH_PERCENTAGE_MIN = 75.0;

    private static final double MASTERED_ACCURACY_MIN = 75.0;
    private static final double MASTERED_EFFECTIVE_ACCURACY_MIN = 80.0;
    private static final double MASTERED_SKIP_RATE_MAX_EXCLUSIVE = 15.0;

    private static final double AVOIDANT_SKIP_RATE_MIN = 45.0;
    private static final double AVOIDANT_SKIP_RATE_WITH_GAP_MIN = 30.0;
    private static final double AVOIDANT_CONFIDENCE_GAP_MIN = 20.0;

    private static final double HESITANT_EFFECTIVE_ACCURACY_MIN = 80.0;
    private static final double HESITANT_SKIP_RATE_MIN = 15.0;
    private static final double HESITANT_CONFIDENCE_GAP_MIN = 15.0;

    private static final double ERROR_PRONE_EFFECTIVE_ACCURACY_MAX_EXCLUSIVE = 65.0;
    private static final double ERROR_PRONE_SKIP_RATE_MAX_EXCLUSIVE = 30.0;

    private static final double DEVELOPING_ACCURACY_MIN = 55.0;
    private static final double DEVELOPING_ACCURACY_MAX_EXCLUSIVE = 75.0;
    private static final double DEVELOPING_EFFECTIVE_ACCURACY_MIN = 65.0;
    private static final double DEVELOPING_EFFECTIVE_ACCURACY_MAX_EXCLUSIVE = 80.0;
    private static final double DEVELOPING_SKIP_RATE_MIN = 15.0;
    private static final double DEVELOPING_SKIP_RATE_MAX_EXCLUSIVE = 30.0;

    private static final double BALANCED_ACCURACY_MIN = 60.0;
    private static final double BALANCED_ACCURACY_MAX_EXCLUSIVE = 75.0;
    private static final double BALANCED_EFFECTIVE_ACCURACY_MIN = 70.0;
    private static final double BALANCED_EFFECTIVE_ACCURACY_MAX_EXCLUSIVE = 85.0;
    private static final double BALANCED_SKIP_RATE_MAX_EXCLUSIVE = 15.0;
    private static final double BALANCED_CONFIDENCE_GAP_MAX_EXCLUSIVE = 15.0;


    private final LearnerAnalyticsSnapshotRepository learnerAnalyticsSnapshotRepository;
    private final UserPracticeSubmissionRepository userPracticeSubmissionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public LearnerAnalyticsSnapshotResponseDto create(LearnerAnalyticsSnapshotCreateRequestDto request) {
        LearnerAnalyticsSnapshot saved = createSnapshot(
                request.getLearnerId(),
                request.getSkill()
        );

        LearnerAnalyticsSnapshotResponseDto responseDto = new LearnerAnalyticsSnapshotResponseDto();
        responseDto.setId(saved.getId());

        return responseDto;
    }

    @Transactional
    public LearnerAnalyticsSnapshot createSnapshot(
            String learnerId,
            PracticeContentSkill skill
    ) {
        User learner = userRepository
                .findById(learnerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found: " + learnerId
                ));

        List<UserPracticeSubmission> submissions = userPracticeSubmissionRepository
                .findByUserIdAndPracticeContent_SkillOrderBySubmittedAtAsc(
                        learnerId,
                        skill
                );

        if (submissions.isEmpty()) {
            throw new IllegalArgumentException(
                    "No submissions found for learner: " + learnerId
                            + " with skill: " + skill
            );
        }

        LearnerAnalyticsSnapshot latestSnapshot = learnerAnalyticsSnapshotRepository
                .findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(learnerId, skill)
                .orElse(null);

        int nextVersionNumber = latestSnapshot != null
                ? latestSnapshot.getVersionNumber() + 1
                : 1;

        LearnerAnalyticsSnapshot snapshot = new LearnerAnalyticsSnapshot();
        snapshot.setUser(learner);
        snapshot.setSkill(skill);
        snapshot.setVersionNumber(nextVersionNumber);
        snapshot.setSubmissionCountUsed(submissions.size());
        snapshot.getSubmissions().addAll(submissions);

        double rollingAccuracyRate = submissions.stream()
                .mapToDouble(submission -> safeDouble(submission.getAccuracyRate()))
                .average()
                .orElse(0.0);

        double rollingSkipRate = submissions.stream()
                .mapToDouble(submission -> safeDouble(submission.getSkipRate()))
                .average()
                .orElse(0.0);

        double rollingEffectiveAccuracy = submissions.stream()
                .mapToDouble(submission -> safeDouble(submission.getEffectiveAccuracy()))
                .average()
                .orElse(0.0);

        snapshot.setRollingAccuracyRate(roundToOneDecimal(rollingAccuracyRate));
        snapshot.setRollingSkipRate(roundToOneDecimal(rollingSkipRate));
        snapshot.setRollingEffectiveAccuracy(roundToOneDecimal(rollingEffectiveAccuracy));

        Map<PracticeQuestionType, LearnerQuestionTypeAnalytics> questionTypeAnalyticsMap =
                new EnumMap<>(PracticeQuestionType.class);

        Map<PracticeTopicTag, LearnerTopicTagAnalytics> topicTagAnalyticsMap =
                new EnumMap<>(PracticeTopicTag.class);

        for (UserPracticeSubmission submission : submissions) {
            for (SubmissionQuestionTypeAccuracy submissionTypeAccuracy : submission.getQuestionTypeAccuracies()) {
                PracticeQuestionType questionType = submissionTypeAccuracy.getQuestionType();

                LearnerQuestionTypeAnalytics learnerTypeAnalytics =
                        questionTypeAnalyticsMap.computeIfAbsent(questionType, type -> {
                            LearnerQuestionTypeAnalytics analytics = new LearnerQuestionTypeAnalytics();
                            analytics.setLearnerAnalyticsSnapshot(snapshot);
                            analytics.setQuestionType(type);
                            analytics.setContributedSubmissionCount(0);
                            analytics.setRollingExposureCount(0);
                            analytics.setRollingAnsweredQuestionCount(0);
                            analytics.setRollingCorrectQuestionCount(0);
                            analytics.setRollingWrongQuestionCount(0);
                            analytics.setRollingSkipQuestionCount(0);
                            analytics.setRollingCorrectAnswerPercentage(0.0);
                            analytics.setRollingSkipRate(0.0);
                            analytics.setRollingEffectiveAccuracy(0.0);
                            analytics.setStrengthLabel(AnalyticsStrengthLabel.NEUTRAL);
                            return analytics;
                        });

                learnerTypeAnalytics.setContributedSubmissionCount(
                        learnerTypeAnalytics.getContributedSubmissionCount() + 1
                );
                learnerTypeAnalytics.setRollingExposureCount(
                        learnerTypeAnalytics.getRollingExposureCount() + safeInt(submissionTypeAccuracy.getExposureCount())
                );
                learnerTypeAnalytics.setRollingAnsweredQuestionCount(
                        learnerTypeAnalytics.getRollingAnsweredQuestionCount() + safeInt(submissionTypeAccuracy.getAnsweredQuestionCount())
                );
                learnerTypeAnalytics.setRollingCorrectQuestionCount(
                        learnerTypeAnalytics.getRollingCorrectQuestionCount() + safeInt(submissionTypeAccuracy.getCorrectQuestionCount())
                );
                learnerTypeAnalytics.setRollingWrongQuestionCount(
                        learnerTypeAnalytics.getRollingWrongQuestionCount() + safeInt(submissionTypeAccuracy.getWrongQuestionCount())
                );
                learnerTypeAnalytics.setRollingSkipQuestionCount(
                        learnerTypeAnalytics.getRollingSkipQuestionCount() + safeInt(submissionTypeAccuracy.getSkipQuestionCount())
                );
            }

            for (SubmissionTopicTagAccuracy submissionTopicTagAccuracy : submission.getTopicTagAccuracies()) {
                PracticeTopicTag topicTag = submissionTopicTagAccuracy.getTopicTag();

                LearnerTopicTagAnalytics learnerTopicAnalytics =
                        topicTagAnalyticsMap.computeIfAbsent(topicTag, tag -> {
                            LearnerTopicTagAnalytics analytics = new LearnerTopicTagAnalytics();
                            analytics.setLearnerAnalyticsSnapshot(snapshot);
                            analytics.setTopicTag(tag);
                            analytics.setContributedSubmissionCount(0);
                            analytics.setRollingExposureCount(0);
                            analytics.setRollingAnsweredQuestionCount(0);
                            analytics.setRollingCorrectQuestionCount(0);
                            analytics.setRollingWrongQuestionCount(0);
                            analytics.setRollingSkipQuestionCount(0);
                            analytics.setRollingCorrectAnswerPercentage(0.0);
                            analytics.setRollingSkipRate(0.0);
                            analytics.setRollingEffectiveAccuracy(0.0);
                            analytics.setStrengthLabel(AnalyticsStrengthLabel.NEUTRAL);
                            return analytics;
                        });

                learnerTopicAnalytics.setContributedSubmissionCount(
                        learnerTopicAnalytics.getContributedSubmissionCount() + 1
                );
                learnerTopicAnalytics.setRollingExposureCount(
                        learnerTopicAnalytics.getRollingExposureCount() + safeInt(submissionTopicTagAccuracy.getExposureCount())
                );
                learnerTopicAnalytics.setRollingAnsweredQuestionCount(
                        learnerTopicAnalytics.getRollingAnsweredQuestionCount() + safeInt(submissionTopicTagAccuracy.getAnsweredQuestionCount())
                );
                learnerTopicAnalytics.setRollingCorrectQuestionCount(
                        learnerTopicAnalytics.getRollingCorrectQuestionCount() + safeInt(submissionTopicTagAccuracy.getCorrectQuestionCount())
                );
                learnerTopicAnalytics.setRollingWrongQuestionCount(
                        learnerTopicAnalytics.getRollingWrongQuestionCount() + safeInt(submissionTopicTagAccuracy.getWrongQuestionCount())
                );
                learnerTopicAnalytics.setRollingSkipQuestionCount(
                        learnerTopicAnalytics.getRollingSkipQuestionCount() + safeInt(submissionTopicTagAccuracy.getSkipQuestionCount())
                );
            }
        }

        for (LearnerQuestionTypeAnalytics learnerTypeAnalytics : questionTypeAnalyticsMap.values()) {
            int exposureCount = learnerTypeAnalytics.getRollingExposureCount();
            int answeredQuestionCount = learnerTypeAnalytics.getRollingAnsweredQuestionCount();
            int correctQuestionCount = learnerTypeAnalytics.getRollingCorrectQuestionCount();
            int skipQuestionCount = learnerTypeAnalytics.getRollingSkipQuestionCount();

            double questionTypeRollingCorrectAnswerPercentage = exposureCount == 0
                    ? 0.0
                    : (correctQuestionCount * 100.0) / exposureCount;

            double questionTypeRollingSkipRate = exposureCount == 0
                    ? 0.0
                    : (skipQuestionCount * 100.0) / exposureCount;

            double questionTypeRollingEffectiveAccuracy = answeredQuestionCount == 0
                    ? 0.0
                    : (correctQuestionCount * 100.0) / answeredQuestionCount;

            learnerTypeAnalytics.setRollingCorrectAnswerPercentage(
                    roundToOneDecimal(questionTypeRollingCorrectAnswerPercentage)
            );
            learnerTypeAnalytics.setRollingSkipRate(
                    roundToOneDecimal(questionTypeRollingSkipRate)
            );
            learnerTypeAnalytics.setRollingEffectiveAccuracy(
                    roundToOneDecimal(questionTypeRollingEffectiveAccuracy)
            );

            learnerTypeAnalytics.setStrengthLabel(
                    resolveStrengthLabel(learnerTypeAnalytics.getRollingCorrectAnswerPercentage())
            );

            learnerTypeAnalytics.setConclusionLabel(
                    resolveConclusionLabel(
                            learnerTypeAnalytics.getRollingCorrectAnswerPercentage(),
                            learnerTypeAnalytics.getRollingSkipRate(),
                            learnerTypeAnalytics.getRollingEffectiveAccuracy()
                    )
            );

            snapshot.getQuestionTypeAnalytics().add(learnerTypeAnalytics);
        }

        for (LearnerTopicTagAnalytics learnerTopicAnalytics : topicTagAnalyticsMap.values()) {
            int exposureCount = learnerTopicAnalytics.getRollingExposureCount();
            int answeredQuestionCount = learnerTopicAnalytics.getRollingAnsweredQuestionCount();
            int correctQuestionCount = learnerTopicAnalytics.getRollingCorrectQuestionCount();
            int skipQuestionCount = learnerTopicAnalytics.getRollingSkipQuestionCount();

            double topicTagRollingCorrectAnswerPercentage = exposureCount == 0
                    ? 0.0
                    : (correctQuestionCount * 100.0) / exposureCount;

            double topicTagRollingSkipRate = exposureCount == 0
                    ? 0.0
                    : (skipQuestionCount * 100.0) / exposureCount;

            double topicTagRollingEffectiveAccuracy = answeredQuestionCount == 0
                    ? 0.0
                    : (correctQuestionCount * 100.0) / answeredQuestionCount;

            learnerTopicAnalytics.setRollingCorrectAnswerPercentage(
                    roundToOneDecimal(topicTagRollingCorrectAnswerPercentage)
            );
            learnerTopicAnalytics.setRollingSkipRate(
                    roundToOneDecimal(topicTagRollingSkipRate)
            );
            learnerTopicAnalytics.setRollingEffectiveAccuracy(
                    roundToOneDecimal(topicTagRollingEffectiveAccuracy)
            );

            learnerTopicAnalytics.setStrengthLabel(
                    resolveStrengthLabel(learnerTopicAnalytics.getRollingCorrectAnswerPercentage())
            );

            learnerTopicAnalytics.setConclusionLabel(
                    resolveConclusionLabel(
                            learnerTopicAnalytics.getRollingCorrectAnswerPercentage(),
                            learnerTopicAnalytics.getRollingSkipRate(),
                            learnerTopicAnalytics.getRollingEffectiveAccuracy()
                    )
            );

            snapshot.getTopicTagAnalytics().add(learnerTopicAnalytics);
        }

        return learnerAnalyticsSnapshotRepository.save(snapshot);
    }

    @Override
    @Transactional
    public Double getCurrentValue(
            LearnerStudyPlanFocusType focusType,
            LearnerStudyPlanTargetMetric targetMetric,
            String userId,
            PracticeContentSkill skill,
            PracticeQuestionType questionType,
            PracticeTopicTag topicTag
    ) {
        LearnerAnalyticsSnapshot snapshot = learnerAnalyticsSnapshotRepository
                .findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(userId, skill)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No learner analytics snapshot found for user: " + userId + " and skill: " + skill
                ));

        return switch (focusType) {
            case QUESTION_TYPE -> getQuestionTypeCurrentValue(snapshot, targetMetric, questionType);
            case TOPIC -> getTopicCurrentValue(snapshot, targetMetric, topicTag);
        };
    }

    private Double getQuestionTypeCurrentValue(
            LearnerAnalyticsSnapshot snapshot,
            LearnerStudyPlanTargetMetric targetMetric,
            PracticeQuestionType questionType
    ) {
        if (questionType == null) {
            throw new IllegalArgumentException("Question type is required when focus type is QUESTION_TYPE");
        }

        LearnerQuestionTypeAnalytics analytics = snapshot.getQuestionTypeAnalytics()
                .stream()
                .filter(item -> item.getQuestionType() == questionType)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No question type analytics found for question type: " + questionType
                ));

        return mapMetricValue(
                targetMetric,
                analytics.getRollingCorrectAnswerPercentage(),
                analytics.getRollingSkipRate(),
                analytics.getRollingEffectiveAccuracy()
        );
    }

    private Double mapMetricValue(
            LearnerStudyPlanTargetMetric targetMetric,
            Double rollingCorrectAnswerPercentage,
            Double rollingSkipRate,
            Double rollingEffectiveAccuracy
    ) {
        return switch (targetMetric) {
            case ROLLING_CORRECT_ANSWER_PERCENTAGE -> safeDouble(rollingCorrectAnswerPercentage);
            case ROLLING_SKIP_RATE -> safeDouble(rollingSkipRate);
            case ROLLING_EFFECTIVE_ACCURACY -> safeDouble(rollingEffectiveAccuracy);
            case ROLLING_OVERALL_BAND_SCORE -> throw new IllegalArgumentException(
                    "ROLLING_OVERALL_BAND_SCORE is only supported for Writing study plans"
            );
        };
    }

    private Double getTopicCurrentValue(
            LearnerAnalyticsSnapshot snapshot,
            LearnerStudyPlanTargetMetric targetMetric,
            PracticeTopicTag topicTag
    ) {
        if (topicTag == null) {
            throw new IllegalArgumentException("Topic tag is required when focus type is TOPIC");
        }

        LearnerTopicTagAnalytics analytics = snapshot.getTopicTagAnalytics()
                .stream()
                .filter(item -> item.getTopicTag() == topicTag)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No topic analytics found for topic tag: " + topicTag
                ));

        return mapMetricValue(
                targetMetric,
                analytics.getRollingCorrectAnswerPercentage(),
                analytics.getRollingSkipRate(),
                analytics.getRollingEffectiveAccuracy()
        );
    }

    private AnalyticsStrengthLabel resolveStrengthLabel(Double percentage) {
        double value = safeDouble(percentage);

        if (value <= WEAK_STRENGTH_PERCENTAGE_MAX) {
            return AnalyticsStrengthLabel.WEAK;
        }

        if (value >= STRONG_STRENGTH_PERCENTAGE_MIN) {
            return AnalyticsStrengthLabel.STRONG;
        }

        return AnalyticsStrengthLabel.NEUTRAL;
    }

    private AnalyticsConclusionLabel resolveConclusionLabel(
            Double rollingCorrectAnswerPercentage,
            Double rollingSkipRate,
            Double rollingEffectiveAccuracy
    ) {
        double accuracy = safeDouble(rollingCorrectAnswerPercentage);
        double skipRate = safeDouble(rollingSkipRate);
        double effectiveAccuracy = safeDouble(rollingEffectiveAccuracy);

        double confidenceGap = effectiveAccuracy - accuracy;

        if (accuracy >= MASTERED_ACCURACY_MIN
                && effectiveAccuracy >= MASTERED_EFFECTIVE_ACCURACY_MIN
                && skipRate < MASTERED_SKIP_RATE_MAX_EXCLUSIVE) {
            return AnalyticsConclusionLabel.MASTERED;
        }

        if (skipRate >= AVOIDANT_SKIP_RATE_MIN
                || (skipRate >= AVOIDANT_SKIP_RATE_WITH_GAP_MIN
                && confidenceGap >= AVOIDANT_CONFIDENCE_GAP_MIN)) {
            return AnalyticsConclusionLabel.AVOIDANT;
        }

        if (effectiveAccuracy >= HESITANT_EFFECTIVE_ACCURACY_MIN
                && (skipRate >= HESITANT_SKIP_RATE_MIN
                || confidenceGap >= HESITANT_CONFIDENCE_GAP_MIN)) {
            return AnalyticsConclusionLabel.HESITANT;
        }

        if (effectiveAccuracy < ERROR_PRONE_EFFECTIVE_ACCURACY_MAX_EXCLUSIVE
                && skipRate < ERROR_PRONE_SKIP_RATE_MAX_EXCLUSIVE) {
            return AnalyticsConclusionLabel.ERROR_PRONE;
        }

        if (accuracy >= DEVELOPING_ACCURACY_MIN
                && accuracy < DEVELOPING_ACCURACY_MAX_EXCLUSIVE
                && effectiveAccuracy >= DEVELOPING_EFFECTIVE_ACCURACY_MIN
                && effectiveAccuracy < DEVELOPING_EFFECTIVE_ACCURACY_MAX_EXCLUSIVE
                && skipRate >= DEVELOPING_SKIP_RATE_MIN
                && skipRate < DEVELOPING_SKIP_RATE_MAX_EXCLUSIVE) {
            return AnalyticsConclusionLabel.DEVELOPING;
        }

        if (accuracy >= BALANCED_ACCURACY_MIN
                && accuracy < BALANCED_ACCURACY_MAX_EXCLUSIVE
                && effectiveAccuracy >= BALANCED_EFFECTIVE_ACCURACY_MIN
                && effectiveAccuracy < BALANCED_EFFECTIVE_ACCURACY_MAX_EXCLUSIVE
                && skipRate < BALANCED_SKIP_RATE_MAX_EXCLUSIVE
                && confidenceGap < BALANCED_CONFIDENCE_GAP_MAX_EXCLUSIVE) {
            return AnalyticsConclusionLabel.BALANCED;
        }

        return AnalyticsConclusionLabel.MIXED;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
