package com.ieltsmastermind.practice.analytics.management.business.service;

import com.ieltsmastermind.practice.analytics.management.business.interfaces.SubmissionAnalyticsService;
import com.ieltsmastermind.practice.analytics.management.domain.dto.SubmissionAnalyticsCreateRequestDto;
import com.ieltsmastermind.practice.analytics.management.domain.dto.SubmissionAnalyticsResponseDto;
import com.ieltsmastermind.practice.analytics.management.domain.dto.SubmissionAnalyticsSkillCountResponseDto;
import com.ieltsmastermind.practice.analytics.management.domain.entity.FocusTypeAnalytics;
import com.ieltsmastermind.practice.analytics.management.domain.entity.SubmissionAnalytics;
import com.ieltsmastermind.practice.analytics.management.domain.enums.AnalyticsStrengthLabel;
import com.ieltsmastermind.practice.analytics.management.persistence.SubmissionAnalyticsRepository;
import com.ieltsmastermind.practice.attempt.management.domain.entity.SubmissionQuestionTypeAccuracy;
import com.ieltsmastermind.practice.attempt.management.domain.entity.SubmissionTopicTagAccuracy;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeSubmission;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeWritingAnswer;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeWritingReview;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeSubmissionRepository;
import com.ieltsmastermind.practice.content.management.domain.entity.PracticeContent;
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
public class SubmissionAnalyticsServiceImpl implements SubmissionAnalyticsService {

    private static final double WEAK_STRENGTH_PERCENTAGE_MAX = 55.0;
    private static final double STRONG_STRENGTH_PERCENTAGE_MIN = 75.0;

    private static final double WEAK_STRENGTH_BAND_SCORE_MAX = 5.5;
    private static final double STRONG_STRENGTH_BAND_SCORE_MIN = 7.5;

    private final SubmissionAnalyticsRepository submissionAnalyticsRepository;
    private final UserPracticeSubmissionRepository userPracticeSubmissionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public SubmissionAnalyticsResponseDto create(SubmissionAnalyticsCreateRequestDto request) {
        SubmissionAnalytics saved = createSnapshot(
                request.getLearnerId(),
                request.getSkill()
        );

        SubmissionAnalyticsResponseDto responseDto = new SubmissionAnalyticsResponseDto();
        responseDto.setId(saved.getId());

        return responseDto;
    }

    @Override
    @Transactional
    public SubmissionAnalytics createSnapshot(
            String learnerId,
            PracticeContentSkill skill
    ) {
        User learner = userRepository
                .findById(learnerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found: " + learnerId
                ));

        if (skill == PracticeContentSkill.WRITING) {
            return createWritingSnapshot(learner, learnerId);
        }

        return createListeningReadingSnapshot(learner, learnerId, skill);
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
        SubmissionAnalytics snapshot = submissionAnalyticsRepository
                .findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(userId, skill)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No submission analytics found for user: " + userId
                                + " and skill: " + skill
                ));

        return switch (targetMetric) {
            case ROLLING_CORRECT_ANSWER_PERCENTAGE ->
                    getCorrectAnswerPercentageCurrentValue(
                            snapshot,
                            focusType,
                            questionType,
                            topicTag
                    );

            case ROLLING_OVERALL_BAND_SCORE ->
                    getOverallBandScoreCurrentValue(
                            snapshot,
                            focusType,
                            questionType,
                            topicTag
                    );

            case ROLLING_SKIP_RATE, ROLLING_EFFECTIVE_ACCURACY ->
                    throw new IllegalArgumentException(
                            targetMetric + " is not supported by SubmissionAnalytics"
                    );
        };
    }

    @Override
    @Transactional
    public SubmissionAnalyticsSkillCountResponseDto getAnalyticsSubmissionCountsByUserId(String userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found: " + userId
                ));

        SubmissionAnalyticsSkillCountResponseDto dto =
                new SubmissionAnalyticsSkillCountResponseDto();

        dto.setListeningCount(countSnapshotEligibleSubmissions(userId, PracticeContentSkill.LISTENING));
        dto.setReadingCount(countSnapshotEligibleSubmissions(userId, PracticeContentSkill.READING));
        dto.setWritingCount(countSnapshotEligibleSubmissions(userId, PracticeContentSkill.WRITING));
        dto.setSpeakingCount(countSnapshotEligibleSubmissions(userId, PracticeContentSkill.SPEAKING));

        return dto;
    }

    private SubmissionAnalytics createListeningReadingSnapshot(
            User learner,
            String learnerId,
            PracticeContentSkill skill
    ) {
        List<UserPracticeSubmission> submissions =
                findSnapshotEligibleSubmissions(learnerId, skill);

        if (submissions.isEmpty()) {
            throw new IllegalArgumentException(
                    "No submissions found for learner: " + learnerId
                            + " with skill: " + skill
            );
        }

        SubmissionAnalytics snapshot = new SubmissionAnalytics();
        snapshot.setUser(learner);
        snapshot.setSkill(skill);
        snapshot.setVersionNumber(getNextVersionNumber(learnerId, skill));
        snapshot.setSubmissionCountUsed(submissions.size());

        double rollingCorrectAnswerPercentage = submissions.stream()
                .mapToDouble(submission -> safeDouble(submission.getAccuracyRate()))
                .average()
                .orElse(0.0);

        snapshot.setRollingCorrectAnswerPercentage(
                roundToOneDecimal(rollingCorrectAnswerPercentage)
        );
        snapshot.setRollingOverallBandScore(0.0);

        Map<PracticeQuestionType, FocusTypeAnalytics> questionTypeAnalyticsMap =
                new EnumMap<>(PracticeQuestionType.class);

        Map<PracticeQuestionType, Integer> questionTypeCorrectCountMap =
                new EnumMap<>(PracticeQuestionType.class);

        Map<PracticeTopicTag, FocusTypeAnalytics> topicTagAnalyticsMap =
                new EnumMap<>(PracticeTopicTag.class);

        Map<PracticeTopicTag, Integer> topicTagCorrectCountMap =
                new EnumMap<>(PracticeTopicTag.class);

        for (UserPracticeSubmission submission : submissions) {
            collectQuestionTypePercentageAnalytics(
                    snapshot,
                    submission,
                    questionTypeAnalyticsMap,
                    questionTypeCorrectCountMap
            );

            collectTopicTagPercentageAnalytics(
                    snapshot,
                    submission,
                    topicTagAnalyticsMap,
                    topicTagCorrectCountMap
            );
        }

        finalizeQuestionTypePercentageAnalytics(
                snapshot,
                questionTypeAnalyticsMap,
                questionTypeCorrectCountMap
        );

        finalizeTopicTagPercentageAnalytics(
                snapshot,
                topicTagAnalyticsMap,
                topicTagCorrectCountMap
        );

        return submissionAnalyticsRepository.save(snapshot);
    }

    private SubmissionAnalytics createWritingSnapshot(
            User learner,
            String learnerId
    ) {
        List<UserPracticeSubmission> submissions =
                findSnapshotEligibleSubmissions(learnerId, PracticeContentSkill.WRITING);

        if (submissions.isEmpty()) {
            throw new IllegalArgumentException(
                    "No reviewed writing submissions found for learner: " + learnerId
            );
        }

        SubmissionAnalytics snapshot = new SubmissionAnalytics();
        snapshot.setUser(learner);
        snapshot.setSkill(PracticeContentSkill.WRITING);
        snapshot.setVersionNumber(getNextVersionNumber(learnerId, PracticeContentSkill.WRITING));
        snapshot.setSubmissionCountUsed(submissions.size());

        double rollingOverallBandScore = submissions.stream()
                .mapToDouble(this::getOverallTutorBand)
                .average()
                .orElse(0.0);

        snapshot.setRollingOverallBandScore(
                roundToOneDecimal(rollingOverallBandScore)
        );
        snapshot.setRollingCorrectAnswerPercentage(0.0);

        Map<PracticeQuestionType, FocusTypeAnalytics> questionTypeAnalyticsMap =
                new EnumMap<>(PracticeQuestionType.class);

        Map<PracticeQuestionType, Double> questionTypeBandScoreSumMap =
                new EnumMap<>(PracticeQuestionType.class);

        Map<PracticeTopicTag, FocusTypeAnalytics> topicTagAnalyticsMap =
                new EnumMap<>(PracticeTopicTag.class);

        Map<PracticeTopicTag, Double> topicTagBandScoreSumMap =
                new EnumMap<>(PracticeTopicTag.class);

        for (UserPracticeSubmission submission : submissions) {
            double submissionBandScore = getOverallTutorBand(submission);
            PracticeContent practiceContent = getPracticeContent(submission);

            PracticeQuestionType questionType = getSingleQuestionTypeTag(practiceContent);
            PracticeTopicTag topicTag = getSingleTopicTag(practiceContent);

            FocusTypeAnalytics questionTypeAnalytics =
                    questionTypeAnalyticsMap.computeIfAbsent(questionType, type -> {
                        FocusTypeAnalytics analytics = new FocusTypeAnalytics();

                        analytics.setSubmissionAnalytics(snapshot);
                        analytics.setFocusType(LearnerStudyPlanFocusType.QUESTION_TYPE);
                        analytics.setQuestionType(type);
                        analytics.setTopicTag(null);
                        analytics.setContributedSubmissionCount(0);
                        analytics.setRollingExposureCount(0);
                        analytics.setRollingCorrectAnswerPercentage(0.0);
                        analytics.setRollingOverallBandScore(0.0);
                        analytics.setStrengthLabel(AnalyticsStrengthLabel.NEUTRAL);

                        return analytics;
                    });

            questionTypeAnalytics.setContributedSubmissionCount(
                    questionTypeAnalytics.getContributedSubmissionCount() + 1
            );

            questionTypeBandScoreSumMap.merge(
                    questionType,
                    submissionBandScore,
                    Double::sum
            );

            FocusTypeAnalytics topicTagAnalytics =
                    topicTagAnalyticsMap.computeIfAbsent(topicTag, tag -> {
                        FocusTypeAnalytics analytics = new FocusTypeAnalytics();

                        analytics.setSubmissionAnalytics(snapshot);
                        analytics.setFocusType(LearnerStudyPlanFocusType.TOPIC);
                        analytics.setQuestionType(null);
                        analytics.setTopicTag(tag);
                        analytics.setContributedSubmissionCount(0);
                        analytics.setRollingExposureCount(0);
                        analytics.setRollingCorrectAnswerPercentage(0.0);
                        analytics.setRollingOverallBandScore(0.0);
                        analytics.setStrengthLabel(AnalyticsStrengthLabel.NEUTRAL);

                        return analytics;
                    });

            topicTagAnalytics.setContributedSubmissionCount(
                    topicTagAnalytics.getContributedSubmissionCount() + 1
            );

            topicTagBandScoreSumMap.merge(
                    topicTag,
                    submissionBandScore,
                    Double::sum
            );
        }

        finalizeQuestionTypeBandScoreAnalytics(
                snapshot,
                questionTypeAnalyticsMap,
                questionTypeBandScoreSumMap
        );

        finalizeTopicTagBandScoreAnalytics(
                snapshot,
                topicTagAnalyticsMap,
                topicTagBandScoreSumMap
        );

        return submissionAnalyticsRepository.save(snapshot);
    }

    private void collectQuestionTypePercentageAnalytics(
            SubmissionAnalytics snapshot,
            UserPracticeSubmission submission,
            Map<PracticeQuestionType, FocusTypeAnalytics> questionTypeAnalyticsMap,
            Map<PracticeQuestionType, Integer> questionTypeCorrectCountMap
    ) {
        for (SubmissionQuestionTypeAccuracy submissionTypeAccuracy
                : submission.getQuestionTypeAccuracies()) {

            PracticeQuestionType questionType = submissionTypeAccuracy.getQuestionType();

            if (questionType == null) {
                continue;
            }

            FocusTypeAnalytics analytics = questionTypeAnalyticsMap.computeIfAbsent(
                    questionType,
                    type -> {
                        FocusTypeAnalytics item = new FocusTypeAnalytics();

                        item.setSubmissionAnalytics(snapshot);
                        item.setFocusType(LearnerStudyPlanFocusType.QUESTION_TYPE);
                        item.setQuestionType(type);
                        item.setTopicTag(null);
                        item.setContributedSubmissionCount(0);
                        item.setRollingExposureCount(0);
                        item.setRollingCorrectAnswerPercentage(0.0);
                        item.setRollingOverallBandScore(0.0);
                        item.setStrengthLabel(AnalyticsStrengthLabel.NEUTRAL);

                        return item;
                    }
            );

            analytics.setContributedSubmissionCount(
                    analytics.getContributedSubmissionCount() + 1
            );

            analytics.setRollingExposureCount(
                    analytics.getRollingExposureCount()
                            + safeInt(submissionTypeAccuracy.getExposureCount())
            );

            questionTypeCorrectCountMap.merge(
                    questionType,
                    safeInt(submissionTypeAccuracy.getCorrectQuestionCount()),
                    Integer::sum
            );
        }
    }

    private void collectTopicTagPercentageAnalytics(
            SubmissionAnalytics snapshot,
            UserPracticeSubmission submission,
            Map<PracticeTopicTag, FocusTypeAnalytics> topicTagAnalyticsMap,
            Map<PracticeTopicTag, Integer> topicTagCorrectCountMap
    ) {
        for (SubmissionTopicTagAccuracy submissionTopicTagAccuracy
                : submission.getTopicTagAccuracies()) {

            PracticeTopicTag topicTag = submissionTopicTagAccuracy.getTopicTag();

            if (topicTag == null) {
                continue;
            }

            FocusTypeAnalytics analytics = topicTagAnalyticsMap.computeIfAbsent(
                    topicTag,
                    tag -> {
                        FocusTypeAnalytics item = new FocusTypeAnalytics();

                        item.setSubmissionAnalytics(snapshot);
                        item.setFocusType(LearnerStudyPlanFocusType.TOPIC);
                        item.setQuestionType(null);
                        item.setTopicTag(tag);
                        item.setContributedSubmissionCount(0);
                        item.setRollingExposureCount(0);
                        item.setRollingCorrectAnswerPercentage(0.0);
                        item.setRollingOverallBandScore(0.0);
                        item.setStrengthLabel(AnalyticsStrengthLabel.NEUTRAL);

                        return item;
                    }
            );

            analytics.setContributedSubmissionCount(
                    analytics.getContributedSubmissionCount() + 1
            );

            analytics.setRollingExposureCount(
                    analytics.getRollingExposureCount()
                            + safeInt(submissionTopicTagAccuracy.getExposureCount())
            );

            topicTagCorrectCountMap.merge(
                    topicTag,
                    safeInt(submissionTopicTagAccuracy.getCorrectQuestionCount()),
                    Integer::sum
            );
        }
    }

    private void finalizeQuestionTypePercentageAnalytics(
            SubmissionAnalytics snapshot,
            Map<PracticeQuestionType, FocusTypeAnalytics> questionTypeAnalyticsMap,
            Map<PracticeQuestionType, Integer> questionTypeCorrectCountMap
    ) {
        for (Map.Entry<PracticeQuestionType, FocusTypeAnalytics> entry
                : questionTypeAnalyticsMap.entrySet()) {

            PracticeQuestionType questionType = entry.getKey();
            FocusTypeAnalytics analytics = entry.getValue();

            int exposureCount = safeInt(analytics.getRollingExposureCount());
            int correctQuestionCount = questionTypeCorrectCountMap.getOrDefault(questionType, 0);

            double percentage = exposureCount == 0
                    ? 0.0
                    : (correctQuestionCount * 100.0) / exposureCount;

            analytics.setRollingCorrectAnswerPercentage(roundToOneDecimal(percentage));
            analytics.setRollingOverallBandScore(0.0);
            analytics.setStrengthLabel(
                    resolvePercentageStrengthLabel(analytics.getRollingCorrectAnswerPercentage())
            );

            snapshot.getFocusTypeAnalytics().add(analytics);
        }
    }

    private void finalizeTopicTagPercentageAnalytics(
            SubmissionAnalytics snapshot,
            Map<PracticeTopicTag, FocusTypeAnalytics> topicTagAnalyticsMap,
            Map<PracticeTopicTag, Integer> topicTagCorrectCountMap
    ) {
        for (Map.Entry<PracticeTopicTag, FocusTypeAnalytics> entry
                : topicTagAnalyticsMap.entrySet()) {

            PracticeTopicTag topicTag = entry.getKey();
            FocusTypeAnalytics analytics = entry.getValue();

            int exposureCount = safeInt(analytics.getRollingExposureCount());
            int correctQuestionCount = topicTagCorrectCountMap.getOrDefault(topicTag, 0);

            double percentage = exposureCount == 0
                    ? 0.0
                    : (correctQuestionCount * 100.0) / exposureCount;

            analytics.setRollingCorrectAnswerPercentage(roundToOneDecimal(percentage));
            analytics.setRollingOverallBandScore(0.0);
            analytics.setStrengthLabel(
                    resolvePercentageStrengthLabel(analytics.getRollingCorrectAnswerPercentage())
            );

            snapshot.getFocusTypeAnalytics().add(analytics);
        }
    }

    private void finalizeQuestionTypeBandScoreAnalytics(
            SubmissionAnalytics snapshot,
            Map<PracticeQuestionType, FocusTypeAnalytics> questionTypeAnalyticsMap,
            Map<PracticeQuestionType, Double> questionTypeBandScoreSumMap
    ) {
        for (Map.Entry<PracticeQuestionType, FocusTypeAnalytics> entry
                : questionTypeAnalyticsMap.entrySet()) {

            PracticeQuestionType questionType = entry.getKey();
            FocusTypeAnalytics analytics = entry.getValue();

            int count = safeInt(analytics.getContributedSubmissionCount());
            double bandScoreSum = questionTypeBandScoreSumMap.getOrDefault(questionType, 0.0);

            double bandScore = count == 0 ? 0.0 : bandScoreSum / count;

            analytics.setRollingOverallBandScore(roundToOneDecimal(bandScore));
            analytics.setRollingCorrectAnswerPercentage(0.0);
            analytics.setRollingExposureCount(count);
            analytics.setStrengthLabel(
                    resolveBandScoreStrengthLabel(analytics.getRollingOverallBandScore())
            );

            snapshot.getFocusTypeAnalytics().add(analytics);
        }
    }

    private void finalizeTopicTagBandScoreAnalytics(
            SubmissionAnalytics snapshot,
            Map<PracticeTopicTag, FocusTypeAnalytics> topicTagAnalyticsMap,
            Map<PracticeTopicTag, Double> topicTagBandScoreSumMap
    ) {
        for (Map.Entry<PracticeTopicTag, FocusTypeAnalytics> entry
                : topicTagAnalyticsMap.entrySet()) {

            PracticeTopicTag topicTag = entry.getKey();
            FocusTypeAnalytics analytics = entry.getValue();

            int count = safeInt(analytics.getContributedSubmissionCount());
            double bandScoreSum = topicTagBandScoreSumMap.getOrDefault(topicTag, 0.0);

            double bandScore = count == 0 ? 0.0 : bandScoreSum / count;

            analytics.setRollingOverallBandScore(roundToOneDecimal(bandScore));
            analytics.setRollingCorrectAnswerPercentage(0.0);
            analytics.setRollingExposureCount(count);
            analytics.setStrengthLabel(
                    resolveBandScoreStrengthLabel(analytics.getRollingOverallBandScore())
            );

            snapshot.getFocusTypeAnalytics().add(analytics);
        }
    }

    private Double getCorrectAnswerPercentageCurrentValue(
            SubmissionAnalytics snapshot,
            LearnerStudyPlanFocusType focusType,
            PracticeQuestionType questionType,
            PracticeTopicTag topicTag
    ) {
        if (focusType == null) {
            return safeDouble(snapshot.getRollingCorrectAnswerPercentage());
        }

        FocusTypeAnalytics analytics = findFocusTypeAnalytics(
                snapshot,
                focusType,
                questionType,
                topicTag
        );

        return safeDouble(analytics.getRollingCorrectAnswerPercentage());
    }

    private Double getOverallBandScoreCurrentValue(
            SubmissionAnalytics snapshot,
            LearnerStudyPlanFocusType focusType,
            PracticeQuestionType questionType,
            PracticeTopicTag topicTag
    ) {
        if (focusType == null) {
            return safeDouble(snapshot.getRollingOverallBandScore());
        }

        FocusTypeAnalytics analytics = findFocusTypeAnalytics(
                snapshot,
                focusType,
                questionType,
                topicTag
        );

        return safeDouble(analytics.getRollingOverallBandScore());
    }

    private FocusTypeAnalytics findFocusTypeAnalytics(
            SubmissionAnalytics snapshot,
            LearnerStudyPlanFocusType focusType,
            PracticeQuestionType questionType,
            PracticeTopicTag topicTag
    ) {
        return switch (focusType) {
            case QUESTION_TYPE -> {
                if (questionType == null) {
                    throw new IllegalArgumentException(
                            "Question type is required when focus type is QUESTION_TYPE"
                    );
                }

                yield snapshot.getFocusTypeAnalytics()
                        .stream()
                        .filter(item -> item.getFocusType() == LearnerStudyPlanFocusType.QUESTION_TYPE)
                        .filter(item -> item.getQuestionType() == questionType)
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No question type analytics found for question type: " + questionType
                        ));
            }

            case TOPIC -> {
                if (topicTag == null) {
                    throw new IllegalArgumentException(
                            "Topic tag is required when focus type is TOPIC"
                    );
                }

                yield snapshot.getFocusTypeAnalytics()
                        .stream()
                        .filter(item -> item.getFocusType() == LearnerStudyPlanFocusType.TOPIC)
                        .filter(item -> item.getTopicTag() == topicTag)
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No topic analytics found for topic tag: " + topicTag
                        ));
            }
        };
    }

    private int getNextVersionNumber(
            String learnerId,
            PracticeContentSkill skill
    ) {
        SubmissionAnalytics latestSnapshot = submissionAnalyticsRepository
                .findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(
                        learnerId,
                        skill
                )
                .orElse(null);

        return latestSnapshot == null
                ? 1
                : latestSnapshot.getVersionNumber() + 1;
    }

    private boolean hasOverallTutorBandScore(UserPracticeSubmission submission) {
        if (submission.getWritingAnswers().isEmpty()) {
            return false;
        }

        UserPracticeWritingAnswer writingAnswer = submission.getWritingAnswers().get(0);

        return !writingAnswer.getWritingReviews().isEmpty();
    }

    private double getOverallTutorBand(UserPracticeSubmission submission) {
        if (submission.getWritingAnswers().isEmpty()) {
            throw new IllegalArgumentException(
                    "No writing answer found for submission: " + submission.getId()
            );
        }

        UserPracticeWritingAnswer writingAnswer = submission.getWritingAnswers().get(0);

        if (writingAnswer.getWritingReviews().isEmpty()) {
            throw new IllegalArgumentException(
                    "No writing review found for writing answer: " + writingAnswer.getId()
            );
        }

        UserPracticeWritingReview writingReview = writingAnswer.getWritingReviews().get(0);

        return safeDouble(writingReview.getOverallTutorBand());
    }

    private PracticeContent getPracticeContent(UserPracticeSubmission submission) {
        if (submission.getPracticeContent() == null) {
            throw new IllegalArgumentException(
                    "No practice content found for submission: " + submission.getId()
            );
        }

        return submission.getPracticeContent();
    }

    private PracticeQuestionType getSingleQuestionTypeTag(PracticeContent practiceContent) {
        if (practiceContent.getQuestionTypeTags().isEmpty()) {
            throw new IllegalArgumentException(
                    "No question type tag found for practice content: " + practiceContent.getId()
            );
        }

        return practiceContent.getQuestionTypeTags().iterator().next();
    }

    private PracticeTopicTag getSingleTopicTag(PracticeContent practiceContent) {
        if (practiceContent.getTopicTags().isEmpty()) {
            throw new IllegalArgumentException(
                    "No topic tag found for practice content: " + practiceContent.getId()
            );
        }

        return practiceContent.getTopicTags().iterator().next();
    }

    private AnalyticsStrengthLabel resolvePercentageStrengthLabel(Double percentage) {
        double value = safeDouble(percentage);

        if (value <= WEAK_STRENGTH_PERCENTAGE_MAX) {
            return AnalyticsStrengthLabel.WEAK;
        }

        if (value >= STRONG_STRENGTH_PERCENTAGE_MIN) {
            return AnalyticsStrengthLabel.STRONG;
        }

        return AnalyticsStrengthLabel.NEUTRAL;
    }

    private AnalyticsStrengthLabel resolveBandScoreStrengthLabel(Double bandScore) {
        double value = safeDouble(bandScore);

        if (value <= WEAK_STRENGTH_BAND_SCORE_MAX) {
            return AnalyticsStrengthLabel.WEAK;
        }

        if (value >= STRONG_STRENGTH_BAND_SCORE_MIN) {
            return AnalyticsStrengthLabel.STRONG;
        }

        return AnalyticsStrengthLabel.NEUTRAL;
    }

    private long countSnapshotEligibleSubmissions(
            String userId,
            PracticeContentSkill skill
    ) {
        return findSnapshotEligibleSubmissions(userId, skill).size();
    }

    private List<UserPracticeSubmission> findSnapshotEligibleSubmissions(
            String userId,
            PracticeContentSkill skill
    ) {
        List<UserPracticeSubmission> submissions =
                userPracticeSubmissionRepository
                        .findByUserIdAndPracticeContent_SkillOrderBySubmittedAtAsc(
                                userId,
                                skill
                        );

        if (skill == PracticeContentSkill.WRITING) {
            return submissions.stream()
                    .filter(this::hasOverallTutorBandScore)
                    .toList();
        }

        return submissions;
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