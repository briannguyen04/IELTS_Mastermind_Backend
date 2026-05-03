package com.ieltsmastermind.practice.analytics.management.business.service;

import com.ieltsmastermind.practice.analytics.management.business.interfaces.LearnerTrendSnapshotService;
import com.ieltsmastermind.practice.analytics.management.domain.dto.LearnerTrendSnapshotCreateRequestDto;
import com.ieltsmastermind.practice.analytics.management.domain.dto.LearnerTrendSnapshotResponseDto;
import com.ieltsmastermind.practice.analytics.management.domain.entity.LearnerQuestionTypeTrend;
import com.ieltsmastermind.practice.analytics.management.domain.entity.LearnerTopicTagTrend;
import com.ieltsmastermind.practice.analytics.management.domain.entity.LearnerTrendSnapshot;
import com.ieltsmastermind.practice.analytics.management.domain.entity.LearnerTrendSnapshotSubmission;
import com.ieltsmastermind.practice.analytics.management.domain.enums.TrendLabel;
import com.ieltsmastermind.practice.analytics.management.persistence.LearnerTrendSnapshotRepository;
import com.ieltsmastermind.practice.attempt.management.domain.entity.SubmissionQuestionTypeAccuracy;
import com.ieltsmastermind.practice.attempt.management.domain.entity.SubmissionTopicTagAccuracy;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeSubmission;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeSubmissionRepository;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTopicTag;
import com.ieltsmastermind.user.management.domain.entity.User;
import com.ieltsmastermind.user.management.persistence.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.hipparchus.stat.regression.SimpleRegression;
import org.springframework.stereotype.Service;


import java.util.*;

@Service
@RequiredArgsConstructor
public class LearnerTrendSnapshotServiceImpl implements LearnerTrendSnapshotService {

    private static final double TREND_POSITIVE_THRESHOLD = 2.0;
    private static final double TREND_NEGATIVE_THRESHOLD = -2.0;

    private final LearnerTrendSnapshotRepository learnerTrendSnapshotRepository;
    private final UserPracticeSubmissionRepository userPracticeSubmissionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public LearnerTrendSnapshotResponseDto create(LearnerTrendSnapshotCreateRequestDto request) {

        LearnerTrendSnapshot saved = createSnapshot(
                request.getLearnerId(),
                request.getSkill()
        );

        LearnerTrendSnapshotResponseDto responseDto = new LearnerTrendSnapshotResponseDto();
        responseDto.setId(saved.getId());

        return responseDto;
    }

    @Transactional
    public LearnerTrendSnapshot createSnapshot(
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

        submissions.sort(Comparator.comparing(UserPracticeSubmission::getSubmittedAt));

        LearnerTrendSnapshot latestSnapshot = learnerTrendSnapshotRepository
                .findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(learnerId, skill)
                .orElse(null);

        int nextVersionNumber = latestSnapshot != null
                ? latestSnapshot.getVersionNumber() + 1
                : 1;

        LearnerTrendSnapshot snapshot = new LearnerTrendSnapshot();
        snapshot.setUser(learner);
        snapshot.setSkill(skill);
        snapshot.setVersionNumber(nextVersionNumber);
        snapshot.setSubmissionCountUsed(submissions.size());

        List<DataPoint> overallAccuracyPoints = new ArrayList<>();
        List<DataPoint> overallSkipRatePoints = new ArrayList<>();
        List<DataPoint> overallEffectiveAccuracyPoints = new ArrayList<>();

        Map<PracticeQuestionType, QuestionTypeTrendAccumulator> questionTypeTrendMap =
                new EnumMap<>(PracticeQuestionType.class);

        Map<PracticeTopicTag, TopicTagTrendAccumulator> topicTagTrendMap =
                new EnumMap<>(PracticeTopicTag.class);

        for (int i = 0; i < submissions.size(); i++) {
            int submissionIndex = i + 1;
            UserPracticeSubmission submission = submissions.get(i);

            LearnerTrendSnapshotSubmission snapshotSubmission = new LearnerTrendSnapshotSubmission();
            snapshotSubmission.setLearnerTrendSnapshot(snapshot);
            snapshotSubmission.setSubmission(submission);
            snapshotSubmission.setSubmissionIndex(submissionIndex);
            snapshot.getSubmissions().add(snapshotSubmission);

            overallAccuracyPoints.add(new DataPoint(submissionIndex, safeDouble(submission.getAccuracyRate())));
            overallSkipRatePoints.add(new DataPoint(submissionIndex, safeDouble(submission.getSkipRate())));
            overallEffectiveAccuracyPoints.add(new DataPoint(submissionIndex, safeDouble(submission.getEffectiveAccuracy())));

            for (SubmissionQuestionTypeAccuracy submissionTypeAccuracy : submission.getQuestionTypeAccuracies()) {
                PracticeQuestionType questionType = submissionTypeAccuracy.getQuestionType();

                QuestionTypeTrendAccumulator accumulator = questionTypeTrendMap.computeIfAbsent(questionType, type -> {
                    LearnerQuestionTypeTrend trend = new LearnerQuestionTypeTrend();
                    trend.setLearnerTrendSnapshot(snapshot);
                    trend.setQuestionType(type);
                    trend.setContributedSubmissionCount(0);
                    trend.setAccuracyTrendLabel(TrendLabel.STABLE);
                    trend.setSkipRateTrendLabel(TrendLabel.STABLE);
                    trend.setEffectiveAccuracyTrendLabel(TrendLabel.STABLE);
                    return new QuestionTypeTrendAccumulator(trend);
                });

                accumulator.trend.setContributedSubmissionCount(
                        accumulator.trend.getContributedSubmissionCount() + 1
                );
                accumulator.accuracyPoints.add(
                        new DataPoint(submissionIndex, safeDouble(submissionTypeAccuracy.getCorrectAnswerPercentage()))
                );
                accumulator.skipRatePoints.add(
                        new DataPoint(submissionIndex, safeDouble(submissionTypeAccuracy.getSkipRate()))
                );
                accumulator.effectiveAccuracyPoints.add(
                        new DataPoint(submissionIndex, safeDouble(submissionTypeAccuracy.getEffectiveAccuracy()))
                );
            }

            for (SubmissionTopicTagAccuracy submissionTopicTagAccuracy : submission.getTopicTagAccuracies()) {
                PracticeTopicTag topicTag = submissionTopicTagAccuracy.getTopicTag();

                TopicTagTrendAccumulator accumulator = topicTagTrendMap.computeIfAbsent(topicTag, tag -> {
                    LearnerTopicTagTrend trend = new LearnerTopicTagTrend();
                    trend.setLearnerTrendSnapshot(snapshot);
                    trend.setTopicTag(tag);
                    trend.setContributedSubmissionCount(0);
                    trend.setAccuracyTrendLabel(TrendLabel.STABLE);
                    trend.setSkipRateTrendLabel(TrendLabel.STABLE);
                    trend.setEffectiveAccuracyTrendLabel(TrendLabel.STABLE);
                    return new TopicTagTrendAccumulator(trend);
                });

                accumulator.trend.setContributedSubmissionCount(
                        accumulator.trend.getContributedSubmissionCount() + 1
                );
                accumulator.accuracyPoints.add(
                        new DataPoint(submissionIndex, safeDouble(submissionTopicTagAccuracy.getCorrectAnswerPercentage()))
                );
                accumulator.skipRatePoints.add(
                        new DataPoint(submissionIndex, safeDouble(submissionTopicTagAccuracy.getSkipRate()))
                );
                accumulator.effectiveAccuracyPoints.add(
                        new DataPoint(submissionIndex, safeDouble(submissionTopicTagAccuracy.getEffectiveAccuracy()))
                );
            }
        }

        RegressionStats overallAccuracyStats = calculateTrendStats(overallAccuracyPoints, false);
        RegressionStats overallSkipRateStats = calculateTrendStats(overallSkipRatePoints, true);
        RegressionStats overallEffectiveAccuracyStats = calculateTrendStats(overallEffectiveAccuracyPoints, false);

        applyOverallAccuracyStats(snapshot, overallAccuracyStats);
        applyOverallSkipRateStats(snapshot, overallSkipRateStats);
        applyOverallEffectiveAccuracyStats(snapshot, overallEffectiveAccuracyStats);

        for (QuestionTypeTrendAccumulator accumulator : questionTypeTrendMap.values()) {
            RegressionStats accuracyStats = calculateTrendStats(accumulator.accuracyPoints, false);
            RegressionStats skipRateStats = calculateTrendStats(accumulator.skipRatePoints, true);
            RegressionStats effectiveAccuracyStats = calculateTrendStats(accumulator.effectiveAccuracyPoints, false);

            applyQuestionTypeAccuracyStats(accumulator.trend, accuracyStats);
            applyQuestionTypeSkipRateStats(accumulator.trend, skipRateStats);
            applyQuestionTypeEffectiveAccuracyStats(accumulator.trend, effectiveAccuracyStats);

            snapshot.getQuestionTypeTrends().add(accumulator.trend);
        }

        for (TopicTagTrendAccumulator accumulator : topicTagTrendMap.values()) {
            RegressionStats accuracyStats = calculateTrendStats(accumulator.accuracyPoints, false);
            RegressionStats skipRateStats = calculateTrendStats(accumulator.skipRatePoints, true);
            RegressionStats effectiveAccuracyStats = calculateTrendStats(accumulator.effectiveAccuracyPoints, false);

            applyTopicTagAccuracyStats(accumulator.trend, accuracyStats);
            applyTopicTagSkipRateStats(accumulator.trend, skipRateStats);
            applyTopicTagEffectiveAccuracyStats(accumulator.trend, effectiveAccuracyStats);

            snapshot.getTopicTagTrends().add(accumulator.trend);
        }

        return learnerTrendSnapshotRepository.save(snapshot);
    }

    private RegressionStats calculateTrendStats(List<DataPoint> points, boolean reverseInterpretation) {
        RegressionStats stats = new RegressionStats();
        stats.dataPointCount = points.size();

        if (points.isEmpty()) {
            stats.trendLabel = TrendLabel.STABLE;
            return stats;
        }

        double xSum = 0.0;
        double ySum = 0.0;

        for (DataPoint point : points) {
            xSum += point.x;
            ySum += point.y;
        }

        stats.xMean = xSum / points.size();
        stats.yMean = ySum / points.size();

        SimpleRegression regression = new SimpleRegression();

        double numerator = 0.0;
        double denominator = 0.0;

        for (DataPoint point : points) {
            regression.addData(point.x, point.y);

            numerator += (point.x - stats.xMean) * (point.y - stats.yMean);
            denominator += Math.pow(point.x - stats.xMean, 2);
        }

        stats.numerator = numerator;
        stats.denominator = denominator;
        stats.slope = denominator == 0.0 ? 0.0 : numerator / denominator;
        stats.trendLabel = resolveTrendLabel(stats.slope, reverseInterpretation);

        return stats;
    }

    private TrendLabel resolveTrendLabel(Double slope, boolean reverseInterpretation) {
        double value = safeDouble(slope);

        if (!reverseInterpretation) {
            if (value > TREND_POSITIVE_THRESHOLD) {
                return TrendLabel.IMPROVING;
            }
            if (value < TREND_NEGATIVE_THRESHOLD) {
                return TrendLabel.DECLINING;
            }
            return TrendLabel.STABLE;
        }

        if (value > TREND_POSITIVE_THRESHOLD) {
            return TrendLabel.DECLINING;
        }
        if (value < TREND_NEGATIVE_THRESHOLD) {
            return TrendLabel.IMPROVING;
        }
        return TrendLabel.STABLE;
    }

    private void applyOverallAccuracyStats(LearnerTrendSnapshot snapshot, RegressionStats stats) {
        snapshot.setOverallAccuracyDataPointCount(stats.dataPointCount);
        snapshot.setOverallAccuracyXMean(stats.xMean);
        snapshot.setOverallAccuracyYMean(stats.yMean);
        snapshot.setOverallAccuracyNumerator(stats.numerator);
        snapshot.setOverallAccuracyDenominator(stats.denominator);
        snapshot.setOverallAccuracySlope(roundToFourDecimals(stats.slope));
        snapshot.setOverallAccuracyTrendLabel(stats.trendLabel);
    }

    private void applyOverallSkipRateStats(LearnerTrendSnapshot snapshot, RegressionStats stats) {
        snapshot.setOverallSkipRateDataPointCount(stats.dataPointCount);
        snapshot.setOverallSkipRateXMean(stats.xMean);
        snapshot.setOverallSkipRateYMean(stats.yMean);
        snapshot.setOverallSkipRateNumerator(stats.numerator);
        snapshot.setOverallSkipRateDenominator(stats.denominator);
        snapshot.setOverallSkipRateSlope(roundToFourDecimals(stats.slope));
        snapshot.setOverallSkipRateTrendLabel(stats.trendLabel);
    }

    private void applyOverallEffectiveAccuracyStats(LearnerTrendSnapshot snapshot, RegressionStats stats) {
        snapshot.setOverallEffectiveAccuracyDataPointCount(stats.dataPointCount);
        snapshot.setOverallEffectiveAccuracyXMean(stats.xMean);
        snapshot.setOverallEffectiveAccuracyYMean(stats.yMean);
        snapshot.setOverallEffectiveAccuracyNumerator(stats.numerator);
        snapshot.setOverallEffectiveAccuracyDenominator(stats.denominator);
        snapshot.setOverallEffectiveAccuracySlope(roundToFourDecimals(stats.slope));
        snapshot.setOverallEffectiveAccuracyTrendLabel(stats.trendLabel);
    }

    private void applyQuestionTypeAccuracyStats(LearnerQuestionTypeTrend trend, RegressionStats stats) {
        trend.setAccuracyDataPointCount(stats.dataPointCount);
        trend.setAccuracyXMean(stats.xMean);
        trend.setAccuracyYMean(stats.yMean);
        trend.setAccuracyNumerator(stats.numerator);
        trend.setAccuracyDenominator(stats.denominator);
        trend.setAccuracySlope(roundToFourDecimals(stats.slope));
        trend.setAccuracyTrendLabel(stats.trendLabel);
    }

    private void applyQuestionTypeSkipRateStats(LearnerQuestionTypeTrend trend, RegressionStats stats) {
        trend.setSkipRateDataPointCount(stats.dataPointCount);
        trend.setSkipRateXMean(stats.xMean);
        trend.setSkipRateYMean(stats.yMean);
        trend.setSkipRateNumerator(stats.numerator);
        trend.setSkipRateDenominator(stats.denominator);
        trend.setSkipRateSlope(roundToFourDecimals(stats.slope));
        trend.setSkipRateTrendLabel(stats.trendLabel);
    }

    private void applyQuestionTypeEffectiveAccuracyStats(LearnerQuestionTypeTrend trend, RegressionStats stats) {
        trend.setEffectiveAccuracyDataPointCount(stats.dataPointCount);
        trend.setEffectiveAccuracyXMean(stats.xMean);
        trend.setEffectiveAccuracyYMean(stats.yMean);
        trend.setEffectiveAccuracyNumerator(stats.numerator);
        trend.setEffectiveAccuracyDenominator(stats.denominator);
        trend.setEffectiveAccuracySlope(roundToFourDecimals(stats.slope));
        trend.setEffectiveAccuracyTrendLabel(stats.trendLabel);
    }

    private void applyTopicTagAccuracyStats(LearnerTopicTagTrend trend, RegressionStats stats) {
        trend.setAccuracyDataPointCount(stats.dataPointCount);
        trend.setAccuracyXMean(stats.xMean);
        trend.setAccuracyYMean(stats.yMean);
        trend.setAccuracyNumerator(stats.numerator);
        trend.setAccuracyDenominator(stats.denominator);
        trend.setAccuracySlope(roundToFourDecimals(stats.slope));
        trend.setAccuracyTrendLabel(stats.trendLabel);
    }

    private void applyTopicTagSkipRateStats(LearnerTopicTagTrend trend, RegressionStats stats) {
        trend.setSkipRateDataPointCount(stats.dataPointCount);
        trend.setSkipRateXMean(stats.xMean);
        trend.setSkipRateYMean(stats.yMean);
        trend.setSkipRateNumerator(stats.numerator);
        trend.setSkipRateDenominator(stats.denominator);
        trend.setSkipRateSlope(roundToFourDecimals(stats.slope));
        trend.setSkipRateTrendLabel(stats.trendLabel);
    }

    private void applyTopicTagEffectiveAccuracyStats(LearnerTopicTagTrend trend, RegressionStats stats) {
        trend.setEffectiveAccuracyDataPointCount(stats.dataPointCount);
        trend.setEffectiveAccuracyXMean(stats.xMean);
        trend.setEffectiveAccuracyYMean(stats.yMean);
        trend.setEffectiveAccuracyNumerator(stats.numerator);
        trend.setEffectiveAccuracyDenominator(stats.denominator);
        trend.setEffectiveAccuracySlope(roundToFourDecimals(stats.slope));
        trend.setEffectiveAccuracyTrendLabel(stats.trendLabel);
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private double roundToFourDecimals(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private static class DataPoint {
        private final int x;
        private final double y;

        private DataPoint(int x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private static class RegressionStats {
        private int dataPointCount = 0;
        private double xMean = 0.0;
        private double yMean = 0.0;
        private double numerator = 0.0;
        private double denominator = 0.0;
        private double slope = 0.0;
        private TrendLabel trendLabel = TrendLabel.STABLE;
    }

    private static class QuestionTypeTrendAccumulator {
        private final LearnerQuestionTypeTrend trend;
        private final List<DataPoint> accuracyPoints = new ArrayList<>();
        private final List<DataPoint> skipRatePoints = new ArrayList<>();
        private final List<DataPoint> effectiveAccuracyPoints = new ArrayList<>();

        private QuestionTypeTrendAccumulator(LearnerQuestionTypeTrend trend) {
            this.trend = trend;
        }
    }

    private static class TopicTagTrendAccumulator {
        private final LearnerTopicTagTrend trend;
        private final List<DataPoint> accuracyPoints = new ArrayList<>();
        private final List<DataPoint> skipRatePoints = new ArrayList<>();
        private final List<DataPoint> effectiveAccuracyPoints = new ArrayList<>();

        private TopicTagTrendAccumulator(LearnerTopicTagTrend trend) {
            this.trend = trend;
        }
    }
}
