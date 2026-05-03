package com.ieltsmastermind.practice.analytics.management.business.service;

import com.ieltsmastermind.practice.analytics.management.business.interfaces.LearnerWritingAnalyticsSnapshotService;
import com.ieltsmastermind.practice.analytics.management.domain.dto.LearnerWritingAnalyticsSnapshotCreateRequestDto;
import com.ieltsmastermind.practice.analytics.management.domain.dto.LearnerWritingAnalyticsSnapshotResponseDto;
import com.ieltsmastermind.practice.analytics.management.domain.entity.LearnerWritingAnalyticsSnapshot;
import com.ieltsmastermind.practice.analytics.management.domain.entity.LearnerWritingQuestionTypeAnalytics;
import com.ieltsmastermind.practice.analytics.management.domain.entity.LearnerWritingTopicTagAnalytics;
import com.ieltsmastermind.practice.analytics.management.domain.enums.AnalyticsStrengthLabel;
import com.ieltsmastermind.practice.analytics.management.persistence.LearnerWritingAnalyticsSnapshotRepository;
import com.ieltsmastermind.practice.attempt.management.domain.entity.*;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LearnerWritingAnalyticsSnapshotServiceImpl implements LearnerWritingAnalyticsSnapshotService {

    private static final double WEAK_STRENGTH_BAND_SCORE_MAX = 5.5;
    private static final double STRONG_STRENGTH_BAND_SCORE_MIN = 7.5;

    private final LearnerWritingAnalyticsSnapshotRepository learnerWritingAnalyticsSnapshotRepository;
    private final UserPracticeSubmissionRepository userPracticeSubmissionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public LearnerWritingAnalyticsSnapshotResponseDto create(
            LearnerWritingAnalyticsSnapshotCreateRequestDto request
    ) {
        LearnerWritingAnalyticsSnapshot saved = createSnapshot(request.getLearnerId());

        LearnerWritingAnalyticsSnapshotResponseDto responseDto =
                new LearnerWritingAnalyticsSnapshotResponseDto();

        responseDto.setId(saved.getId());

        return responseDto;
    }

    @Override
    @Transactional
    public LearnerWritingAnalyticsSnapshot createSnapshot(String learnerId) {
        User learner = userRepository
                .findById(learnerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found: " + learnerId
                ));

        List<UserPracticeSubmission> allSubmissions = userPracticeSubmissionRepository
                .findByUserIdAndPracticeContent_SkillOrderBySubmittedAtAsc(
                        learnerId,
                        PracticeContentSkill.WRITING
                );

        List<UserPracticeSubmission> submissions = allSubmissions.stream()
                .filter(this::hasOverallTutorBandScore)
                .toList();

        if (submissions.isEmpty()) {
            throw new IllegalArgumentException(
                    "No reviewed writing submissions found for learner: " + learnerId
            );
        }

        LearnerWritingAnalyticsSnapshot latestSnapshot = learnerWritingAnalyticsSnapshotRepository
                .findTopByUser_UserIdOrderByVersionNumberDesc(learnerId)
                .orElse(null);

        int nextVersionNumber = latestSnapshot != null
                ? latestSnapshot.getVersionNumber() + 1
                : 1;

        LearnerWritingAnalyticsSnapshot snapshot = new LearnerWritingAnalyticsSnapshot();
        snapshot.setUser(learner);
        snapshot.setVersionNumber(nextVersionNumber);
        snapshot.setSubmissionCountUsed(submissions.size());
        snapshot.getSubmissions().addAll(submissions);

        double rollingOverallBandScore = submissions.stream()
                .mapToDouble(this::getOverallTutorBand)
                .average()
                .orElse(0.0);

        snapshot.setRollingOverallBandScore(roundToOneDecimal(rollingOverallBandScore));

        Map<PracticeQuestionType, LearnerWritingQuestionTypeAnalytics> questionTypeAnalyticsMap =
                new EnumMap<>(PracticeQuestionType.class);

        Map<PracticeQuestionType, Double> questionTypeBandScoreSumMap =
                new EnumMap<>(PracticeQuestionType.class);

        Map<PracticeTopicTag, LearnerWritingTopicTagAnalytics> topicTagAnalyticsMap =
                new EnumMap<>(PracticeTopicTag.class);

        Map<PracticeTopicTag, Double> topicTagBandScoreSumMap =
                new EnumMap<>(PracticeTopicTag.class);

        for (UserPracticeSubmission submission : submissions) {
            double submissionBandScore = getOverallTutorBand(submission);
            PracticeContent practiceContent = getPracticeContent(submission);

            PracticeQuestionType questionType = getSingleQuestionTypeTag(practiceContent);
            PracticeTopicTag topicTag = getSingleTopicTag(practiceContent);

            LearnerWritingQuestionTypeAnalytics learnerTypeAnalytics =
                    questionTypeAnalyticsMap.computeIfAbsent(questionType, type -> {
                        LearnerWritingQuestionTypeAnalytics analytics =
                                new LearnerWritingQuestionTypeAnalytics();

                        analytics.setLearnerWritingAnalyticsSnapshot(snapshot);
                        analytics.setQuestionType(type);
                        analytics.setContributedSubmissionCount(0);
                        analytics.setRollingOverallBandScore(0.0);
                        analytics.setStrengthLabel(AnalyticsStrengthLabel.NEUTRAL);

                        return analytics;
                    });

            learnerTypeAnalytics.setContributedSubmissionCount(
                    learnerTypeAnalytics.getContributedSubmissionCount() + 1
            );

            questionTypeBandScoreSumMap.merge(
                    questionType,
                    submissionBandScore,
                    Double::sum
            );

            LearnerWritingTopicTagAnalytics learnerTopicAnalytics =
                    topicTagAnalyticsMap.computeIfAbsent(topicTag, tag -> {
                        LearnerWritingTopicTagAnalytics analytics =
                                new LearnerWritingTopicTagAnalytics();

                        analytics.setLearnerWritingAnalyticsSnapshot(snapshot);
                        analytics.setTopicTag(tag);
                        analytics.setContributedSubmissionCount(0);
                        analytics.setRollingOverallBandScore(0.0);
                        analytics.setStrengthLabel(AnalyticsStrengthLabel.NEUTRAL);

                        return analytics;
                    });

            learnerTopicAnalytics.setContributedSubmissionCount(
                    learnerTopicAnalytics.getContributedSubmissionCount() + 1
            );

            topicTagBandScoreSumMap.merge(
                    topicTag,
                    submissionBandScore,
                    Double::sum
            );
        }

        for (Map.Entry<PracticeQuestionType, LearnerWritingQuestionTypeAnalytics> entry
                : questionTypeAnalyticsMap.entrySet()) {
            PracticeQuestionType questionType = entry.getKey();
            LearnerWritingQuestionTypeAnalytics analytics = entry.getValue();

            double bandScoreSum = questionTypeBandScoreSumMap.getOrDefault(questionType, 0.0);
            int contributedSubmissionCount = analytics.getContributedSubmissionCount();

            double rollingQuestionTypeBandScore = contributedSubmissionCount == 0
                    ? 0.0
                    : bandScoreSum / contributedSubmissionCount;

            analytics.setRollingOverallBandScore(
                    roundToOneDecimal(rollingQuestionTypeBandScore)
            );

            analytics.setStrengthLabel(
                    resolveStrengthLabel(analytics.getRollingOverallBandScore())
            );

            snapshot.getQuestionTypeAnalytics().add(analytics);
        }

        for (Map.Entry<PracticeTopicTag, LearnerWritingTopicTagAnalytics> entry
                : topicTagAnalyticsMap.entrySet()) {
            PracticeTopicTag topicTag = entry.getKey();
            LearnerWritingTopicTagAnalytics analytics = entry.getValue();

            double bandScoreSum = topicTagBandScoreSumMap.getOrDefault(topicTag, 0.0);
            int contributedSubmissionCount = analytics.getContributedSubmissionCount();

            double rollingTopicTagBandScore = contributedSubmissionCount == 0
                    ? 0.0
                    : bandScoreSum / contributedSubmissionCount;

            analytics.setRollingOverallBandScore(
                    roundToOneDecimal(rollingTopicTagBandScore)
            );

            analytics.setStrengthLabel(
                    resolveStrengthLabel(analytics.getRollingOverallBandScore())
            );

            snapshot.getTopicTagAnalytics().add(analytics);
        }

        return learnerWritingAnalyticsSnapshotRepository.save(snapshot);
    }

    @Override
    @Transactional
    public Double getCurrentValue(
            LearnerStudyPlanFocusType focusType,
            LearnerStudyPlanTargetMetric targetMetric,
            String userId,
            PracticeQuestionType questionType,
            PracticeTopicTag topicTag
    ) {
        if (targetMetric != LearnerStudyPlanTargetMetric.ROLLING_OVERALL_BAND_SCORE) {
            throw new IllegalArgumentException(
                    "Writing analytics only supports ROLLING_OVERALL_BAND_SCORE"
            );
        }

        LearnerWritingAnalyticsSnapshot snapshot = learnerWritingAnalyticsSnapshotRepository
                .findTopByUser_UserIdOrderByVersionNumberDesc(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No learner writing analytics snapshot found for user: " + userId
                ));

        return switch (focusType) {
            case QUESTION_TYPE -> getQuestionTypeCurrentValue(snapshot, questionType);
            case TOPIC -> getTopicCurrentValue(snapshot, topicTag);
        };
    }

    private Double getQuestionTypeCurrentValue(
            LearnerWritingAnalyticsSnapshot snapshot,
            PracticeQuestionType questionType
    ) {
        if (questionType == null) {
            throw new IllegalArgumentException(
                    "Question type is required when focus type is QUESTION_TYPE"
            );
        }

        LearnerWritingQuestionTypeAnalytics analytics = snapshot.getQuestionTypeAnalytics()
                .stream()
                .filter(item -> item.getQuestionType() == questionType)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No writing question type analytics found for question type: " + questionType
                ));

        return safeDouble(analytics.getRollingOverallBandScore());
    }

    private Double getTopicCurrentValue(
            LearnerWritingAnalyticsSnapshot snapshot,
            PracticeTopicTag topicTag
    ) {
        if (topicTag == null) {
            throw new IllegalArgumentException(
                    "Topic tag is required when focus type is TOPIC"
            );
        }

        LearnerWritingTopicTagAnalytics analytics = snapshot.getTopicTagAnalytics()
                .stream()
                .filter(item -> item.getTopicTag() == topicTag)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No writing topic tag analytics found for topic tag: " + topicTag
                ));

        return safeDouble(analytics.getRollingOverallBandScore());
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

    private AnalyticsStrengthLabel resolveStrengthLabel(Double bandScore) {
        double value = safeDouble(bandScore);

        if (value <= WEAK_STRENGTH_BAND_SCORE_MAX) {
            return AnalyticsStrengthLabel.WEAK;
        }

        if (value >= STRONG_STRENGTH_BAND_SCORE_MIN) {
            return AnalyticsStrengthLabel.STRONG;
        }

        return AnalyticsStrengthLabel.NEUTRAL;
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
