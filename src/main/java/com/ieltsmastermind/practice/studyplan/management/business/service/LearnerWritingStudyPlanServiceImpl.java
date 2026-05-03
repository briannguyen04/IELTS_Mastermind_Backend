package com.ieltsmastermind.practice.studyplan.management.business.service;

import com.ieltsmastermind.practice.analytics.management.business.interfaces.LearnerWritingAnalyticsSnapshotService;
import com.ieltsmastermind.practice.analytics.management.business.interfaces.LearnerWritingTrendSnapshotService;
import com.ieltsmastermind.practice.analytics.management.domain.entity.*;
import com.ieltsmastermind.practice.analytics.management.domain.enums.AnalyticsStrengthLabel;
import com.ieltsmastermind.practice.analytics.management.domain.enums.TrendLabel;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeSubmission;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeWritingAnswer;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeSubmissionRepository;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTopicTag;
import com.ieltsmastermind.practice.studyplan.management.business.interfaces.LearnerWritingStudyPlanService;
import com.ieltsmastermind.practice.studyplan.management.domain.dto.LearnerStudyPlanResponseDto;
import com.ieltsmastermind.practice.studyplan.management.domain.dto.LearnerWritingStudyPlanCreateRequestDto;
import com.ieltsmastermind.practice.studyplan.management.domain.entity.LearnerStudyPlan;
import com.ieltsmastermind.practice.studyplan.management.domain.entity.LearnerStudyPlanStrengthBlock;
import com.ieltsmastermind.practice.studyplan.management.domain.entity.LearnerStudyPlanTask;
import com.ieltsmastermind.practice.studyplan.management.domain.entity.LearnerStudyPlanWeaknessBlock;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.*;
import com.ieltsmastermind.practice.studyplan.management.persistence.LearnerStudyPlanRepository;
import com.ieltsmastermind.user.management.domain.entity.User;
import com.ieltsmastermind.user.management.persistence.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearnerWritingStudyPlanServiceImpl implements LearnerWritingStudyPlanService {

    private static final int MAX_STRENGTH_BLOCKS = 4;
    private static final int MAX_STRENGTH_QUESTION_TYPES = 3;
    private static final int MAX_STRENGTH_TOPICS = 1;

    private static final int MAX_WEAKNESS_BLOCKS = 4;
    private static final int MAX_WEAKNESS_QUESTION_TYPES = 3;
    private static final int MAX_WEAKNESS_TOPICS = 1;
    private static final int MAX_WEAKNESS_TASKS = 4;

    private static final int MIN_REVIEWED_SUBMISSION_COUNT = 5;
    private static final int MIN_SUBMISSION_COVERAGE = 2;

    private static final double WEAK_TARGET_STEP = 0.5;
    private static final double NEUTRAL_TARGET_STEP = 0.5;
    private static final double STRONG_TARGET_STEP = 0.0;
    private static final double MAX_BAND_SCORE = 9.0;

    private final LearnerStudyPlanRepository learnerStudyPlanRepository;
    private final UserPracticeSubmissionRepository userPracticeSubmissionRepository;
    private final UserRepository userRepository;

    private final LearnerWritingAnalyticsSnapshotService learnerWritingAnalyticsSnapshotService;
    private final LearnerWritingTrendSnapshotService learnerWritingTrendSnapshotService;

    @Override
    @Transactional
    public LearnerStudyPlanResponseDto createForLearner(String learnerId) {
        User learner = userRepository.findById(learnerId)
                .orElseThrow(() -> new IllegalArgumentException("Learner not found"));

        List<UserPracticeSubmission> submissions = userPracticeSubmissionRepository
                .findByUserIdAndPracticeContent_SkillOrderBySubmittedAtAsc(
                        learner.getUserId(),
                        PracticeContentSkill.WRITING
                )
                .stream()
                .filter(this::hasOverallTutorBandScore)
                .toList();

        if (submissions.size() < MIN_REVIEWED_SUBMISSION_COUNT) {
            throw new IllegalArgumentException(
                    "At least 5 reviewed writing submissions are required for learner"
            );
        }

        LearnerWritingAnalyticsSnapshot analyticsSnapshot =
                learnerWritingAnalyticsSnapshotService.createSnapshot(learner.getUserId());

        LearnerWritingTrendSnapshot trendSnapshot =
                learnerWritingTrendSnapshotService.createSnapshot(learner.getUserId());

        LearnerStudyPlan latestStudyPlan = learnerStudyPlanRepository
                .findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(
                        learner.getUserId(),
                        PracticeContentSkill.WRITING
                )
                .orElse(null);

        int nextVersionNumber = latestStudyPlan != null
                ? latestStudyPlan.getVersionNumber() + 1
                : 1;

        if (latestStudyPlan != null) {
            latestStudyPlan.setStatus(LearnerStudyPlanStatus.INACTIVE);
        }

        LearnerStudyPlan studyPlan = new LearnerStudyPlan();
        studyPlan.setUser(learner);
        studyPlan.setSkill(PracticeContentSkill.WRITING);
        studyPlan.setVersionNumber(nextVersionNumber);
        studyPlan.setStatus(LearnerStudyPlanStatus.ACTIVE);
        studyPlan.setSubmissionCountUsed(submissions.size());
        studyPlan.setSubmissionCountSinceCreation(0);

        studyPlan.setLearnerWritingAnalyticsSnapshot(analyticsSnapshot);
        studyPlan.setLearnerWritingTrendSnapshot(trendSnapshot);

        attachTestingSubmissions(studyPlan, submissions);
        generateStrengthBlocks(studyPlan, analyticsSnapshot, trendSnapshot);
        generateWeaknessBlocks(studyPlan, analyticsSnapshot, trendSnapshot);
        generateTasks(studyPlan, analyticsSnapshot, trendSnapshot);

        LearnerStudyPlan saved = learnerStudyPlanRepository.save(studyPlan);

        LearnerStudyPlanResponseDto responseDto = new LearnerStudyPlanResponseDto();
        responseDto.setId(saved.getId());

        return responseDto;
    }

    @Override
    @Transactional
    public void refreshStudyPlanIfStudyPlanExists(String userId) {
        LearnerStudyPlan studyPlan = learnerStudyPlanRepository
                .findTopByUser_UserIdAndSkillAndStatusOrderByVersionNumberDesc(
                        userId,
                        PracticeContentSkill.WRITING,
                        LearnerStudyPlanStatus.ACTIVE
                )
                .orElse(null);

        if (studyPlan == null) {
            return;
        }

        refreshWritingTaskProgress(studyPlan, userId);
        studyPlan.setReadyToFinalize(resolveReadyToFinalize(studyPlan));

        learnerStudyPlanRepository.save(studyPlan);
    }

    private void refreshWritingTaskProgress(
            LearnerStudyPlan studyPlan,
            String userId
    ) {
        for (LearnerStudyPlanTask task : studyPlan.getTasks()) {
            Double currentValue = learnerWritingAnalyticsSnapshotService.getCurrentValue(
                    task.getFocusType(),
                    task.getTargetMetric(),
                    userId,
                    task.getQuestionType(),
                    task.getTopicTag()
            );

            double safeCurrentValue = currentValue != null ? currentValue : 0.0;
            task.setCurrentValue(safeCurrentValue);
            task.setStatus(resolveTaskStatus(task, safeCurrentValue));
        }
    }

    private LearnerStudyPlanTaskStatus resolveTaskStatus(
            LearnerStudyPlanTask task,
            double currentValue
    ) {
        double targetValue = task.getTargetValue() != null ? task.getTargetValue() : 0.0;
        LearnerStudyPlanTaskDirection direction = task.getDirection();

        boolean completed = switch (direction) {
            case INCREASE -> currentValue >= targetValue;
            case REDUCE -> currentValue <= targetValue;
        };

        return completed
                ? LearnerStudyPlanTaskStatus.COMPLETED
                : LearnerStudyPlanTaskStatus.ACTIVE;
    }

    private boolean resolveReadyToFinalize(LearnerStudyPlan studyPlan) {
        if (studyPlan.getTasks() == null || studyPlan.getTasks().isEmpty()) {
            return false;
        }

        Integer submissionCountSinceCreation = studyPlan.getSubmissionCountSinceCreation();

        if (submissionCountSinceCreation == null
                || submissionCountSinceCreation < MIN_REVIEWED_SUBMISSION_COUNT) {
            return false;
        }

        long completedTaskCount = studyPlan.getTasks()
                .stream()
                .filter(task -> task.getStatus() == LearnerStudyPlanTaskStatus.COMPLETED)
                .count();

        int totalTaskCount = studyPlan.getTasks().size();

        double completedTaskPercentage =
                (double) completedTaskCount * 100.0 / totalTaskCount;

        return completedTaskPercentage >= 75.0;
    }

    private void attachTestingSubmissions(
            LearnerStudyPlan studyPlan,
            List<UserPracticeSubmission> submissions
    ) {
        for (UserPracticeSubmission submission : submissions) {
            submission.setLearnerStudyPlan(studyPlan);
            studyPlan.getSubmissions().add(submission);
        }
    }

    private void generateStrengthBlocks(
            LearnerStudyPlan studyPlan,
            LearnerWritingAnalyticsSnapshot analyticsSnapshot,
            LearnerWritingTrendSnapshot trendSnapshot
    ) {
        List<FocusCandidate> strongQuestionTypeCandidates =
                buildQuestionTypeCandidates(analyticsSnapshot, trendSnapshot).stream()
                        .filter(FocusCandidate::isEligibleForRanking)
                        .filter(candidate -> candidate.getStrengthLabel() == AnalyticsStrengthLabel.STRONG)
                        .sorted(strengthComparator())
                        .limit(MAX_STRENGTH_QUESTION_TYPES)
                        .toList();

        List<FocusCandidate> strongTopicCandidates =
                buildTopicCandidates(analyticsSnapshot, trendSnapshot).stream()
                        .filter(FocusCandidate::isEligibleForRanking)
                        .filter(candidate -> candidate.getStrengthLabel() == AnalyticsStrengthLabel.STRONG)
                        .sorted(strengthComparator())
                        .limit(MAX_STRENGTH_TOPICS)
                        .toList();

        List<FocusCandidate> selected = new ArrayList<>();
        selected.addAll(strongQuestionTypeCandidates);
        selected.addAll(strongTopicCandidates);

        selected = selected.stream()
                .sorted(strengthComparator())
                .limit(MAX_STRENGTH_BLOCKS)
                .toList();

        int rank = 1;
        for (FocusCandidate candidate : selected) {
            LearnerStudyPlanStrengthBlock block = new LearnerStudyPlanStrengthBlock();
            block.setLearnerStudyPlan(studyPlan);
            block.setStrengthRank(rank++);
            block.setFocusType(candidate.getFocusType());
            block.setQuestionType(candidate.getQuestionType());
            block.setTopicTag(candidate.getTopicTag());
            block.setStatus(resolveStrengthBlockStatus(candidate.getBandScoreTrendLabel()));

            studyPlan.getStrengthBlocks().add(block);
        }
    }

    private void generateWeaknessBlocks(
            LearnerStudyPlan studyPlan,
            LearnerWritingAnalyticsSnapshot analyticsSnapshot,
            LearnerWritingTrendSnapshot trendSnapshot
    ) {
        List<FocusCandidate> weakQuestionTypeCandidates =
                buildQuestionTypeCandidates(analyticsSnapshot, trendSnapshot).stream()
                        .filter(FocusCandidate::isEligibleForRanking)
                        .filter(candidate -> candidate.getStrengthLabel() == AnalyticsStrengthLabel.WEAK)
                        .sorted(weaknessComparator())
                        .limit(MAX_WEAKNESS_QUESTION_TYPES)
                        .toList();

        List<FocusCandidate> weakTopicCandidates =
                buildTopicCandidates(analyticsSnapshot, trendSnapshot).stream()
                        .filter(FocusCandidate::isEligibleForRanking)
                        .filter(candidate -> candidate.getStrengthLabel() == AnalyticsStrengthLabel.WEAK)
                        .sorted(weaknessComparator())
                        .limit(MAX_WEAKNESS_TOPICS)
                        .toList();

        List<FocusCandidate> selected = new ArrayList<>();
        selected.addAll(weakQuestionTypeCandidates);
        selected.addAll(weakTopicCandidates);

        if (selected.size() < MAX_WEAKNESS_BLOCKS) {
            List<FocusCandidate> neutralFillers = buildAllCandidates(analyticsSnapshot, trendSnapshot).stream()
                    .filter(FocusCandidate::isEligibleForRanking)
                    .filter(candidate -> candidate.getStrengthLabel() == AnalyticsStrengthLabel.NEUTRAL)
                    .filter(candidate -> selected.stream().noneMatch(existing -> sameFocus(existing, candidate)))
                    .sorted(weaknessComparator())
                    .limit(MAX_WEAKNESS_BLOCKS - selected.size())
                    .toList();

            selected.addAll(neutralFillers);
        }

        int rank = 1;
        for (FocusCandidate candidate : selected.stream().limit(MAX_WEAKNESS_BLOCKS).toList()) {
            LearnerStudyPlanWeaknessBlock block = new LearnerStudyPlanWeaknessBlock();
            block.setLearnerStudyPlan(studyPlan);
            block.setWeaknessRank(rank++);
            block.setFocusType(candidate.getFocusType());
            block.setQuestionType(candidate.getQuestionType());
            block.setTopicTag(candidate.getTopicTag());
            block.setStatus(resolveWeaknessBlockStatus(candidate.getBandScoreTrendLabel()));

            studyPlan.getWeaknessBlocks().add(block);
        }
    }

    private void generateTasks(
            LearnerStudyPlan studyPlan,
            LearnerWritingAnalyticsSnapshot analyticsSnapshot,
            LearnerWritingTrendSnapshot trendSnapshot
    ) {
        List<FocusCandidate> taskCandidates = buildAllCandidates(analyticsSnapshot, trendSnapshot).stream()
                .filter(FocusCandidate::isEligibleForRanking)
                .filter(candidate ->
                        candidate.getStrengthLabel() == AnalyticsStrengthLabel.WEAK
                                || candidate.getStrengthLabel() == AnalyticsStrengthLabel.NEUTRAL
                )
                .sorted(weaknessComparator())
                .limit(MAX_WEAKNESS_TASKS)
                .toList();

        int rank = 1;
        for (FocusCandidate candidate : taskCandidates) {
            double currentValue = safeDouble(candidate.getRollingOverallBandScore());
            double targetValue = resolveTargetValue(currentValue, candidate.getStrengthLabel());

            LearnerStudyPlanTask task = new LearnerStudyPlanTask();
            task.setLearnerStudyPlan(studyPlan);
            task.setPriorityRank(rank++);
            task.setFocusType(candidate.getFocusType());
            task.setQuestionType(candidate.getQuestionType());
            task.setTopicTag(candidate.getTopicTag());
            task.setTargetMetric(LearnerStudyPlanTargetMetric.ROLLING_OVERALL_BAND_SCORE);
            task.setCurrentValue(currentValue);
            task.setTargetValue(targetValue);
            task.setDirection(LearnerStudyPlanTaskDirection.INCREASE);
            task.setStatus(LearnerStudyPlanTaskStatus.ACTIVE);

            studyPlan.getTasks().add(task);
        }
    }

    private List<FocusCandidate> buildAllCandidates(
            LearnerWritingAnalyticsSnapshot analyticsSnapshot,
            LearnerWritingTrendSnapshot trendSnapshot
    ) {
        List<FocusCandidate> candidates = new ArrayList<>();
        candidates.addAll(buildQuestionTypeCandidates(analyticsSnapshot, trendSnapshot));
        candidates.addAll(buildTopicCandidates(analyticsSnapshot, trendSnapshot));
        return candidates;
    }

    private List<FocusCandidate> buildQuestionTypeCandidates(
            LearnerWritingAnalyticsSnapshot analyticsSnapshot,
            LearnerWritingTrendSnapshot trendSnapshot
    ) {
        Map<PracticeQuestionType, LearnerWritingQuestionTypeTrend> trendMap =
                trendSnapshot.getQuestionTypeTrends().stream()
                        .collect(Collectors.toMap(
                                LearnerWritingQuestionTypeTrend::getQuestionType,
                                Function.identity()
                        ));

        return analyticsSnapshot.getQuestionTypeAnalytics().stream()
                .map(analytics -> toQuestionTypeCandidate(
                        analytics,
                        trendMap.get(analytics.getQuestionType())
                ))
                .toList();
    }

    private List<FocusCandidate> buildTopicCandidates(
            LearnerWritingAnalyticsSnapshot analyticsSnapshot,
            LearnerWritingTrendSnapshot trendSnapshot
    ) {
        Map<PracticeTopicTag, LearnerWritingTopicTagTrend> trendMap =
                trendSnapshot.getTopicTagTrends().stream()
                        .collect(Collectors.toMap(
                                LearnerWritingTopicTagTrend::getTopicTag,
                                Function.identity()
                        ));

        return analyticsSnapshot.getTopicTagAnalytics().stream()
                .map(analytics -> toTopicCandidate(
                        analytics,
                        trendMap.get(analytics.getTopicTag())
                ))
                .toList();
    }

    private FocusCandidate toQuestionTypeCandidate(
            LearnerWritingQuestionTypeAnalytics analytics,
            LearnerWritingQuestionTypeTrend trend
    ) {
        return new FocusCandidate(
                LearnerStudyPlanFocusType.QUESTION_TYPE,
                analytics.getQuestionType(),
                null,
                analytics.getContributedSubmissionCount(),
                analytics.getRollingOverallBandScore(),
                analytics.getStrengthLabel(),
                trend != null ? trend.getBandScoreSlope() : 0.0,
                trend != null ? trend.getBandScoreTrendLabel() : TrendLabel.STABLE
        );
    }

    private FocusCandidate toTopicCandidate(
            LearnerWritingTopicTagAnalytics analytics,
            LearnerWritingTopicTagTrend trend
    ) {
        return new FocusCandidate(
                LearnerStudyPlanFocusType.TOPIC,
                null,
                analytics.getTopicTag(),
                analytics.getContributedSubmissionCount(),
                analytics.getRollingOverallBandScore(),
                analytics.getStrengthLabel(),
                trend != null ? trend.getBandScoreSlope() : 0.0,
                trend != null ? trend.getBandScoreTrendLabel() : TrendLabel.STABLE
        );
    }

    private Comparator<FocusCandidate> strengthComparator() {
        return Comparator.comparing(FocusCandidate::getRollingOverallBandScore).reversed();
    }

    private Comparator<FocusCandidate> weaknessComparator() {
        return Comparator.comparing(FocusCandidate::getRollingOverallBandScore);
    }

    private LearnerStudyPlanStrengthBlockStatus resolveStrengthBlockStatus(TrendLabel trendLabel) {
        return switch (trendLabel) {
            case IMPROVING -> LearnerStudyPlanStrengthBlockStatus.IMPROVED;
            case DECLINING -> LearnerStudyPlanStrengthBlockStatus.WEAKENED;
            default -> LearnerStudyPlanStrengthBlockStatus.MAINTAINED;
        };
    }

    private LearnerStudyPlanWeaknessBlockStatus resolveWeaknessBlockStatus(TrendLabel trendLabel) {
        return switch (trendLabel) {
            case IMPROVING -> LearnerStudyPlanWeaknessBlockStatus.IMPROVED;
            case DECLINING -> LearnerStudyPlanWeaknessBlockStatus.WORSENED;
            default -> LearnerStudyPlanWeaknessBlockStatus.CONTINUED;
        };
    }

    private double resolveTargetValue(
            double currentValue,
            AnalyticsStrengthLabel strengthLabel
    ) {
        double step = switch (strengthLabel) {
            case WEAK -> WEAK_TARGET_STEP;
            case NEUTRAL -> NEUTRAL_TARGET_STEP;
            case STRONG -> STRONG_TARGET_STEP;
        };

        return Math.min(MAX_BAND_SCORE, currentValue + step);
    }

    private boolean sameFocus(FocusCandidate left, FocusCandidate right) {
        if (left.getFocusType() != right.getFocusType()) {
            return false;
        }

        if (left.getFocusType() == LearnerStudyPlanFocusType.QUESTION_TYPE) {
            return Objects.equals(left.getQuestionType(), right.getQuestionType());
        }

        return Objects.equals(left.getTopicTag(), right.getTopicTag());
    }

    private boolean hasOverallTutorBandScore(UserPracticeSubmission submission) {
        if (submission.getWritingAnswers().isEmpty()) {
            return false;
        }

        UserPracticeWritingAnswer writingAnswer = submission.getWritingAnswers().get(0);

        return !writingAnswer.getWritingReviews().isEmpty();
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    @Getter
    @AllArgsConstructor
    private static class FocusCandidate {

        private LearnerStudyPlanFocusType focusType;
        private PracticeQuestionType questionType;
        private PracticeTopicTag topicTag;
        private Integer contributedSubmissionCount;
        private Double rollingOverallBandScore;
        private AnalyticsStrengthLabel strengthLabel;
        private Double bandScoreSlope;
        private TrendLabel bandScoreTrendLabel;

        boolean isEligibleForRanking() {
//            return contributedSubmissionCount != null
//                    && contributedSubmissionCount >= MIN_SUBMISSION_COVERAGE;
            return true;
        }
    }
}
