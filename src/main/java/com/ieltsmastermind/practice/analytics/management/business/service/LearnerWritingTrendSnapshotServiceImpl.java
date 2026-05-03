package com.ieltsmastermind.practice.analytics.management.business.service;

import com.ieltsmastermind.practice.analytics.management.business.interfaces.LearnerWritingTrendSnapshotService;
import com.ieltsmastermind.practice.analytics.management.domain.dto.LearnerWritingTrendSnapshotCreateRequestDto;
import com.ieltsmastermind.practice.analytics.management.domain.dto.LearnerWritingTrendSnapshotResponseDto;
import com.ieltsmastermind.practice.analytics.management.domain.entity.LearnerWritingQuestionTypeTrend;
import com.ieltsmastermind.practice.analytics.management.domain.entity.LearnerWritingTopicTagTrend;
import com.ieltsmastermind.practice.analytics.management.domain.entity.LearnerWritingTrendSnapshot;
import com.ieltsmastermind.practice.analytics.management.domain.enums.TrendLabel;
import com.ieltsmastermind.practice.analytics.management.persistence.LearnerWritingTrendSnapshotRepository;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeSubmission;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeWritingAnswer;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeWritingReview;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeSubmissionRepository;
import com.ieltsmastermind.practice.content.management.domain.entity.PracticeContent;
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
public class LearnerWritingTrendSnapshotServiceImpl implements LearnerWritingTrendSnapshotService {

    private static final double TREND_POSITIVE_THRESHOLD = 0.02;
    private static final double TREND_NEGATIVE_THRESHOLD = -0.02;

    private final LearnerWritingTrendSnapshotRepository learnerWritingTrendSnapshotRepository;
    private final UserPracticeSubmissionRepository userPracticeSubmissionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public LearnerWritingTrendSnapshotResponseDto create(
            LearnerWritingTrendSnapshotCreateRequestDto request
    ) {
        LearnerWritingTrendSnapshot saved = createSnapshot(request.getLearnerId());

        LearnerWritingTrendSnapshotResponseDto responseDto =
                new LearnerWritingTrendSnapshotResponseDto();

        responseDto.setId(saved.getId());

        return responseDto;
    }

    @Override
    @Transactional
    public LearnerWritingTrendSnapshot createSnapshot(String learnerId) {
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
                .sorted(Comparator.comparing(UserPracticeSubmission::getSubmittedAt))
                .toList();

        if (submissions.isEmpty()) {
            throw new IllegalArgumentException(
                    "No reviewed writing submissions found for learner: " + learnerId
            );
        }

        LearnerWritingTrendSnapshot latestSnapshot = learnerWritingTrendSnapshotRepository
                .findTopByUser_UserIdOrderByVersionNumberDesc(learnerId)
                .orElse(null);

        int nextVersionNumber = latestSnapshot != null
                ? latestSnapshot.getVersionNumber() + 1
                : 1;

        LearnerWritingTrendSnapshot snapshot = new LearnerWritingTrendSnapshot();
        snapshot.setUser(learner);
        snapshot.setVersionNumber(nextVersionNumber);
        snapshot.setSubmissionCountUsed(submissions.size());
        snapshot.getSubmissions().addAll(submissions);

        List<DataPoint> overallBandScorePoints = new ArrayList<>();

        Map<PracticeQuestionType, QuestionTypeTrendAccumulator> questionTypeTrendMap =
                new EnumMap<>(PracticeQuestionType.class);

        Map<PracticeTopicTag, TopicTagTrendAccumulator> topicTagTrendMap =
                new EnumMap<>(PracticeTopicTag.class);

        for (int i = 0; i < submissions.size(); i++) {
            int submissionIndex = i + 1;
            UserPracticeSubmission submission = submissions.get(i);

            double overallBandScore = getOverallTutorBand(submission);

            overallBandScorePoints.add(
                    new DataPoint(submissionIndex, overallBandScore)
            );

            PracticeContent practiceContent = getPracticeContent(submission);
            PracticeQuestionType questionType = getSingleQuestionTypeTag(practiceContent);
            PracticeTopicTag topicTag = getSingleTopicTag(practiceContent);

            QuestionTypeTrendAccumulator questionTypeAccumulator =
                    questionTypeTrendMap.computeIfAbsent(questionType, type -> {
                        LearnerWritingQuestionTypeTrend trend =
                                new LearnerWritingQuestionTypeTrend();

                        trend.setLearnerWritingTrendSnapshot(snapshot);
                        trend.setQuestionType(type);
                        trend.setContributedSubmissionCount(0);
                        trend.setBandScoreTrendLabel(TrendLabel.STABLE);

                        return new QuestionTypeTrendAccumulator(trend);
                    });

            questionTypeAccumulator.trend.setContributedSubmissionCount(
                    questionTypeAccumulator.trend.getContributedSubmissionCount() + 1
            );

            questionTypeAccumulator.bandScorePoints.add(
                    new DataPoint(submissionIndex, overallBandScore)
            );

            TopicTagTrendAccumulator topicTagAccumulator =
                    topicTagTrendMap.computeIfAbsent(topicTag, tag -> {
                        LearnerWritingTopicTagTrend trend =
                                new LearnerWritingTopicTagTrend();

                        trend.setLearnerWritingTrendSnapshot(snapshot);
                        trend.setTopicTag(tag);
                        trend.setContributedSubmissionCount(0);
                        trend.setBandScoreTrendLabel(TrendLabel.STABLE);

                        return new TopicTagTrendAccumulator(trend);
                    });

            topicTagAccumulator.trend.setContributedSubmissionCount(
                    topicTagAccumulator.trend.getContributedSubmissionCount() + 1
            );

            topicTagAccumulator.bandScorePoints.add(
                    new DataPoint(submissionIndex, overallBandScore)
            );
        }

        RegressionStats overallBandScoreStats =
                calculateTrendStats(overallBandScorePoints);

        applyOverallBandScoreStats(snapshot, overallBandScoreStats);

        for (QuestionTypeTrendAccumulator accumulator : questionTypeTrendMap.values()) {
            RegressionStats bandScoreStats =
                    calculateTrendStats(accumulator.bandScorePoints);

            applyQuestionTypeBandScoreStats(accumulator.trend, bandScoreStats);

            snapshot.getQuestionTypeTrends().add(accumulator.trend);
        }

        for (TopicTagTrendAccumulator accumulator : topicTagTrendMap.values()) {
            RegressionStats bandScoreStats =
                    calculateTrendStats(accumulator.bandScorePoints);

            applyTopicTagBandScoreStats(accumulator.trend, bandScoreStats);

            snapshot.getTopicTagTrends().add(accumulator.trend);
        }

        return learnerWritingTrendSnapshotRepository.save(snapshot);
    }

    private RegressionStats calculateTrendStats(List<DataPoint> points) {
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
        stats.slope = denominator == 0.0 ? 0.0 : regression.getSlope();
        stats.trendLabel = resolveTrendLabel(stats.slope);

        return stats;
    }

    private TrendLabel resolveTrendLabel(Double slope) {
        double value = safeDouble(slope);

        if (value > TREND_POSITIVE_THRESHOLD) {
            return TrendLabel.IMPROVING;
        }

        if (value < TREND_NEGATIVE_THRESHOLD) {
            return TrendLabel.DECLINING;
        }

        return TrendLabel.STABLE;
    }

    private void applyOverallBandScoreStats(
            LearnerWritingTrendSnapshot snapshot,
            RegressionStats stats
    ) {
        snapshot.setOverallBandScoreDataPointCount(stats.dataPointCount);
        snapshot.setOverallBandScoreXMean(stats.xMean);
        snapshot.setOverallBandScoreYMean(stats.yMean);
        snapshot.setOverallBandScoreNumerator(stats.numerator);
        snapshot.setOverallBandScoreDenominator(stats.denominator);
        snapshot.setOverallBandScoreSlope(roundToFourDecimals(stats.slope));
        snapshot.setOverallBandScoreTrendLabel(stats.trendLabel);
    }

    private void applyQuestionTypeBandScoreStats(
            LearnerWritingQuestionTypeTrend trend,
            RegressionStats stats
    ) {
        trend.setBandScoreDataPointCount(stats.dataPointCount);
        trend.setBandScoreXMean(stats.xMean);
        trend.setBandScoreYMean(stats.yMean);
        trend.setBandScoreNumerator(stats.numerator);
        trend.setBandScoreDenominator(stats.denominator);
        trend.setBandScoreSlope(roundToFourDecimals(stats.slope));
        trend.setBandScoreTrendLabel(stats.trendLabel);
    }

    private void applyTopicTagBandScoreStats(
            LearnerWritingTopicTagTrend trend,
            RegressionStats stats
    ) {
        trend.setBandScoreDataPointCount(stats.dataPointCount);
        trend.setBandScoreXMean(stats.xMean);
        trend.setBandScoreYMean(stats.yMean);
        trend.setBandScoreNumerator(stats.numerator);
        trend.setBandScoreDenominator(stats.denominator);
        trend.setBandScoreSlope(roundToFourDecimals(stats.slope));
        trend.setBandScoreTrendLabel(stats.trendLabel);
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
        private final LearnerWritingQuestionTypeTrend trend;
        private final List<DataPoint> bandScorePoints = new ArrayList<>();

        private QuestionTypeTrendAccumulator(LearnerWritingQuestionTypeTrend trend) {
            this.trend = trend;
        }
    }

    private static class TopicTagTrendAccumulator {
        private final LearnerWritingTopicTagTrend trend;
        private final List<DataPoint> bandScorePoints = new ArrayList<>();

        private TopicTagTrendAccumulator(LearnerWritingTopicTagTrend trend) {
            this.trend = trend;
        }
    }
}
