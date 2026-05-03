package com.ieltsmastermind.practice.studyplan.management.business.service;

import com.ieltsmastermind.ai.feedback.business.interfaces.LearnerStudyPlanAIService;
import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.analytics.management.business.interfaces.LearnerAnalyticsSnapshotService;
import com.ieltsmastermind.practice.analytics.management.business.interfaces.LearnerTrendSnapshotService;
import com.ieltsmastermind.practice.analytics.management.business.interfaces.LearnerWritingAnalyticsSnapshotService;
import com.ieltsmastermind.practice.analytics.management.business.interfaces.LearnerWritingTrendSnapshotService;
import com.ieltsmastermind.practice.analytics.management.domain.entity.*;
import com.ieltsmastermind.practice.analytics.management.domain.enums.AnalyticsConclusionLabel;
import com.ieltsmastermind.practice.analytics.management.domain.enums.AnalyticsStrengthLabel;
import com.ieltsmastermind.practice.analytics.management.domain.enums.TrendLabel;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeSubmission;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeSubmissionRepository;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTopicTag;
import com.ieltsmastermind.practice.studyplan.management.business.interfaces.LearnerStudyPlanService;
import com.ieltsmastermind.practice.studyplan.management.business.interfaces.LearnerWritingStudyPlanService;
import com.ieltsmastermind.practice.studyplan.management.domain.dto.*;
import com.ieltsmastermind.practice.studyplan.management.domain.entity.*;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.*;
import com.ieltsmastermind.practice.studyplan.management.persistence.LearnerStudyPlanRepository;
import com.ieltsmastermind.user.management.domain.entity.User;
import com.ieltsmastermind.user.management.persistence.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearnerStudyPlanServiceImpl implements LearnerStudyPlanService {

    private static final int MAX_STRENGTH_BLOCKS = 4;
    private static final int MAX_STRENGTH_QUESTION_TYPES = 3;
    private static final int MAX_STRENGTH_TOPICS = 1;
    private static final int MAX_WEAKNESS_BLOCKS = 4;
    private static final int MAX_WEAKNESS_QUESTION_TYPES = 3;
    private static final int MAX_WEAKNESS_TOPICS = 1;
    private static final int MAX_WEAKNESS_TASKS = 4;

    private static final int MIN_EXPOSURE_COUNT = 8;
    private static final int MIN_SUBMISSION_COVERAGE = 2;

    private static final int MIN_REVIEWED_SUBMISSION_COUNT = 5;
    private static final int MIN_SUBMISSION_COUNT_TO_FINALIZE = 5;
    private static final double MIN_COMPLETED_TASK_PERCENTAGE_TO_FINALIZE = 75.0;

    private static final double WEAK_TARGET_STEP = 8.0;
    private static final double NEUTRAL_TARGET_STEP = 5.0;
    private static final double STRONG_TARGET_STEP = 2.0;

    private final LearnerStudyPlanRepository learnerStudyPlanRepository;
    private final UserPracticeSubmissionRepository userPracticeSubmissionRepository;
    private final UserRepository userRepository;

    private final LearnerAnalyticsSnapshotService learnerAnalyticsSnapshotService;
    private final LearnerTrendSnapshotService learnerTrendSnapshotService;
    private final LearnerWritingStudyPlanService learnerWritingStudyPlanService;
    private final LearnerWritingAnalyticsSnapshotService learnerWritingAnalyticsSnapshotService;
    private final LearnerWritingTrendSnapshotService learnerWritingTrendSnapshotService;

    private final LearnerStudyPlanAIService learnerStudyPlanAIService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;


    @Override
    @Transactional
    public LearnerStudyPlanResponseDto create(LearnerStudyPlanCreateRequestDto request) {
        if (request.getSkill() == PracticeContentSkill.WRITING) {
            return learnerWritingStudyPlanService.createForLearner(request.getLearnerId());
        }

        User learner = userRepository.findById(request.getLearnerId())
                .orElseThrow(() -> new IllegalArgumentException("Learner not found"));

        PracticeContentSkill skill = request.getSkill();

        List<UserPracticeSubmission> submissions =
                userPracticeSubmissionRepository.findByUserIdAndPracticeContent_SkillOrderBySubmittedAtAsc(
                        learner.getUserId(),
                        skill
                );

        if (submissions.size() < MIN_REVIEWED_SUBMISSION_COUNT) {
            throw new IllegalArgumentException("At least 5 submissions are required for learner and skill");
        }

        LearnerAnalyticsSnapshot analyticsSnapshot =
                learnerAnalyticsSnapshotService.createSnapshot(learner.getUserId(), skill);

        LearnerTrendSnapshot trendSnapshot =
                learnerTrendSnapshotService.createSnapshot(learner.getUserId(), skill);

        LearnerStudyPlan latestStudyPlan = learnerStudyPlanRepository
                .findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(learner.getUserId(), skill)
                .orElse(null);

        int nextVersionNumber = latestStudyPlan != null
                ? latestStudyPlan.getVersionNumber() + 1
                : 1;

        if (latestStudyPlan != null) {
            latestStudyPlan.setStatus(LearnerStudyPlanStatus.INACTIVE);
        }

        LearnerStudyPlan studyPlan = new LearnerStudyPlan();
        studyPlan.setUser(learner);
        studyPlan.setSkill(skill);
        studyPlan.setVersionNumber(nextVersionNumber);
        studyPlan.setStatus(LearnerStudyPlanStatus.ACTIVE);
        studyPlan.setSubmissionCountUsed(submissions.size());
        studyPlan.setSubmissionCountSinceCreation(0);
        studyPlan.setLearnerAnalyticsSnapshot(analyticsSnapshot);
        studyPlan.setLearnerTrendSnapshot(trendSnapshot);

        attachTestingSubmissions(studyPlan, submissions);
        generateStrengthBlocks(studyPlan, analyticsSnapshot, trendSnapshot);
        generateWeaknessBlocks(studyPlan, analyticsSnapshot, trendSnapshot);
        generateTasks(studyPlan, analyticsSnapshot, trendSnapshot);

        LearnerStudyPlan saved = learnerStudyPlanRepository.save(studyPlan);

        eventPublisher.publishEvent(
                new StudyPlanCreatedEvent(saved.getId())
        );

        LearnerStudyPlanResponseDto responseDto = new LearnerStudyPlanResponseDto();
        responseDto.setId(saved.getId());

        return responseDto;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(StudyPlanCreatedEvent event) {
        learnerStudyPlanAIService.triggerAIContentGeneration(event.getStudyPlanId());
    }

    @Override
    @Transactional
    public LearnerStudyPlanResponseDto getActiveByUserIdAndSkill(
            String userId,
            PracticeContentSkill skill,
            IncludeSpec includes
    ) {
        LearnerStudyPlan studyPlan = learnerStudyPlanRepository
                .findTopByUser_UserIdAndSkillAndStatusOrderByVersionNumberDesc(
                        userId,
                        skill,
                        LearnerStudyPlanStatus.ACTIVE
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Active learner study plan not found"
                ));

        LearnerStudyPlanResponseDto dto = new LearnerStudyPlanResponseDto();

        if (studyPlan == null) {
            dto.setId("");
            return dto;
        }

        dto.setId(studyPlan.getId());
        applyIncludes(studyPlan, dto, includes);

        return dto;
    }

    @Override
    @Transactional
    public LearnerStudyPlanActiveCheckResponseDto checkHasActiveStudyPlan(
            String userId,
            PracticeContentSkill skill
    ) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Learner not found"));

        boolean hasActiveStudyPlan = learnerStudyPlanRepository.existsByUser_UserIdAndSkillAndStatus(
                userId,
                skill,
                LearnerStudyPlanStatus.ACTIVE
        );

        LearnerStudyPlanActiveCheckResponseDto responseDto = new LearnerStudyPlanActiveCheckResponseDto();
        responseDto.setHasActiveStudyPlan(hasActiveStudyPlan);

        return responseDto;
    }

    @Override
    @Transactional
    public void refreshStudyPlanIfStudyPlanExists(
            String userId,
            PracticeContentSkill skill
    ) {
        if (skill == PracticeContentSkill.WRITING) {
            learnerWritingStudyPlanService.refreshStudyPlanIfStudyPlanExists(userId);
            return;
        }

        LearnerStudyPlan studyPlan = learnerStudyPlanRepository
                .findTopByUser_UserIdAndSkillAndStatusOrderByVersionNumberDesc(
                        userId,
                        skill,
                        LearnerStudyPlanStatus.ACTIVE
                )
                .orElse(null);

        if (studyPlan == null) {
            return;
        }

        refreshTaskProgress(studyPlan, userId, skill);
        studyPlan.setReadyToFinalize(resolveReadyToFinalize(studyPlan));

        learnerStudyPlanRepository.save(studyPlan);
    }

    @Override
    @Transactional
    public void incrementSubmissionCountSinceCreationIfStudyPlanExists(
            String userId,
            PracticeContentSkill skill
    ) {
        LearnerStudyPlan studyPlan = learnerStudyPlanRepository
                .findTopByUser_UserIdAndSkillAndStatusOrderByVersionNumberDesc(
                        userId,
                        skill,
                        LearnerStudyPlanStatus.ACTIVE
                )
                .orElse(null);

        if (studyPlan == null) {
            return;
        }

        Integer submissionCountSinceCreation = studyPlan.getSubmissionCountSinceCreation();

        studyPlan.setSubmissionCountSinceCreation(
                submissionCountSinceCreation != null
                        ? submissionCountSinceCreation + 1
                        : 1
        );

        learnerStudyPlanRepository.save(studyPlan);
    }

    @Override
    @Transactional
    public LearnerStudyPlanResponseDto finalizeById(String id) {
        LearnerStudyPlan studyPlan = learnerStudyPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Learner study plan not found"));

        studyPlan.setStatus(LearnerStudyPlanStatus.INACTIVE);

        LearnerStudyPlan saved = learnerStudyPlanRepository.save(studyPlan);

        LearnerStudyPlanResponseDto responseDto = new LearnerStudyPlanResponseDto();
        responseDto.setId(saved.getId());

        return responseDto;
    }

    @Override
    @Transactional
    public LearnerStudyPlanRefreshAfterSubmissionResponseDto refreshAfterSubmission(
            String userId,
            PracticeContentSkill skill
    ) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Learner not found"));

        if (skill == PracticeContentSkill.WRITING) {
            learnerWritingAnalyticsSnapshotService.createSnapshot(userId);
            learnerWritingTrendSnapshotService.createSnapshot(userId);
        } else {
            learnerAnalyticsSnapshotService.createSnapshot(userId, skill);
            learnerTrendSnapshotService.createSnapshot(userId, skill);
        }

        incrementSubmissionCountSinceCreationIfStudyPlanExists(userId, skill);

        refreshStudyPlanIfStudyPlanExists(userId, skill);

        LearnerStudyPlanRefreshAfterSubmissionResponseDto responseDto =
                new LearnerStudyPlanRefreshAfterSubmissionResponseDto();

        responseDto.setUserId(userId);

        return responseDto;
    }

    private void applyIncludes(
            LearnerStudyPlan studyPlan,
            LearnerStudyPlanResponseDto dto,
            IncludeSpec includes
    ) {
        if (includes.has("skill")) dto.setSkill(studyPlan.getSkill());
        if (includes.has("versionnumber")) dto.setVersionNumber(studyPlan.getVersionNumber());
        if (includes.has("status")) dto.setStatus(studyPlan.getStatus());
        if (includes.has("submissioncountused")) dto.setSubmissionCountUsed(studyPlan.getSubmissionCountUsed());
        if (includes.has("submissioncountsincecreation")) dto.setSubmissionCountSinceCreation(studyPlan.getSubmissionCountSinceCreation());
        if (includes.has("readytofinalize")) dto.setReadyToFinalize(studyPlan.isReadyToFinalize());

        boolean hasUserInclude =
                includes.has("user.userid")
                        || includes.has("user.email")
                        || includes.has("user.firstname")
                        || includes.has("user.lastname");

        if (hasUserInclude && studyPlan.getUser() != null) {
            LearnerStudyPlanUserResponseDto userDto = new LearnerStudyPlanUserResponseDto();

            if (includes.has("user.userid")) userDto.setUserId(studyPlan.getUser().getUserId());
            if (includes.has("user.email")) userDto.setEmail(studyPlan.getUser().getEmail());
            if (includes.has("user.firstname")) userDto.setFirstname(studyPlan.getUser().getFirstname());
            if (includes.has("user.lastname")) userDto.setLastname(studyPlan.getUser().getLastname());

            dto.setUser(userDto);
        }

        boolean hasTasksInclude =
                includes.has("tasks.id")
                        || includes.has("tasks.focustype")
                        || includes.has("tasks.questiontype")
                        || includes.has("tasks.topictag")
                        || includes.has("tasks.targetmetric")
                        || includes.has("tasks.currentvalue")
                        || includes.has("tasks.targetvalue")
                        || includes.has("tasks.direction")
                        || includes.has("tasks.status")
                        || includes.has("tasks.description");

        if (hasTasksInclude) {
            List<LearnerStudyPlanTaskResponseDto> taskDtos = new ArrayList<>();

            String userId = studyPlan.getUser().getUserId();
            PracticeContentSkill skill = studyPlan.getSkill();

            for (LearnerStudyPlanTask task : studyPlan.getTasks()) {
                LearnerStudyPlanTaskResponseDto taskDto = new LearnerStudyPlanTaskResponseDto();

                if (includes.has("tasks.id")) taskDto.setId(task.getId());
                if (includes.has("tasks.focustype")) taskDto.setFocusType(task.getFocusType());
                if (includes.has("tasks.questiontype")) taskDto.setQuestionType(task.getQuestionType());
                if (includes.has("tasks.topictag")) taskDto.setTopicTag(task.getTopicTag());
                if (includes.has("tasks.targetmetric")) taskDto.setTargetMetric(task.getTargetMetric());
                if (includes.has("tasks.currentvalue")) taskDto.setCurrentValue(task.getCurrentValue());
                if (includes.has("tasks.targetvalue")) taskDto.setTargetValue(task.getTargetValue());
                if (includes.has("tasks.direction")) taskDto.setDirection(task.getDirection());
                if (includes.has("tasks.status")) taskDto.setStatus(task.getStatus());
                if (includes.has("tasks.description")) taskDto.setDescription(task.getDescription());

                taskDtos.add(taskDto);
            }

            dto.setTasks(taskDtos);
        }

        boolean hasStrengthBlocksInclude =
                includes.has("strengthblocks.id")
                        || includes.has("strengthblocks.focustype")
                        || includes.has("strengthblocks.questiontype")
                        || includes.has("strengthblocks.topictag")
                        || includes.has("strengthblocks.status")
                        || includes.has("strengthblocks.analyticsconclusionlabel")
                        || includes.has("strengthblocks.description")
                        || includes.has("strengthblocks.explanation")
                        || includes.has("strengthblocks.evidence")
                        || includes.has("strengthblocks.recommendednextaction");

        if (hasStrengthBlocksInclude) {
            List<LearnerStudyPlanStrengthBlockResponseDto> strengthBlockDtos = new ArrayList<>();

            for (LearnerStudyPlanStrengthBlock strengthBlock : studyPlan.getStrengthBlocks()) {
                LearnerStudyPlanStrengthBlockResponseDto strengthBlockDto =
                        new LearnerStudyPlanStrengthBlockResponseDto();

                if (includes.has("strengthblocks.id")) strengthBlockDto.setId(strengthBlock.getId());
                if (includes.has("strengthblocks.focustype")) strengthBlockDto.setFocusType(strengthBlock.getFocusType());
                if (includes.has("strengthblocks.questiontype")) strengthBlockDto.setQuestionType(strengthBlock.getQuestionType());
                if (includes.has("strengthblocks.topictag")) strengthBlockDto.setTopicTag(strengthBlock.getTopicTag());
                if (includes.has("strengthblocks.status")) strengthBlockDto.setStatus(strengthBlock.getStatus());
                if (includes.has("strengthblocks.analyticsconclusionlabel")) {
                    strengthBlockDto.setAnalyticsConclusionLabel(strengthBlock.getAnalyticsConclusionLabel());
                }
                if (includes.has("strengthblocks.description")) strengthBlockDto.setDescription(strengthBlock.getDescription());
                if (includes.has("strengthblocks.explanation")) strengthBlockDto.setExplanation(strengthBlock.getExplanation());
                if (includes.has("strengthblocks.evidence")) strengthBlockDto.setEvidence(strengthBlock.getEvidence());
                if (includes.has("strengthblocks.recommendednextaction")) {
                    strengthBlockDto.setRecommendedNextAction(strengthBlock.getRecommendedNextAction());
                }

                strengthBlockDtos.add(strengthBlockDto);
            }

            dto.setStrengthBlocks(strengthBlockDtos);
        }

        boolean hasWeaknessBlocksInclude =
                includes.has("weaknessblocks.id")
                        || includes.has("weaknessblocks.focustype")
                        || includes.has("weaknessblocks.questiontype")
                        || includes.has("weaknessblocks.topictag")
                        || includes.has("weaknessblocks.status")
                        || includes.has("weaknessblocks.analyticsconclusionlabel")
                        || includes.has("weaknessblocks.description")
                        || includes.has("weaknessblocks.explanation")
                        || includes.has("weaknessblocks.evidence")
                        || includes.has("weaknessblocks.recommendednextaction");

        if (hasWeaknessBlocksInclude) {
            List<LearnerStudyPlanWeaknessBlockResponseDto> weaknessBlockDtos = new ArrayList<>();

            for (LearnerStudyPlanWeaknessBlock weaknessBlock : studyPlan.getWeaknessBlocks()) {
                LearnerStudyPlanWeaknessBlockResponseDto weaknessBlockDto =
                        new LearnerStudyPlanWeaknessBlockResponseDto();

                if (includes.has("weaknessblocks.id")) weaknessBlockDto.setId(weaknessBlock.getId());
                if (includes.has("weaknessblocks.focustype")) weaknessBlockDto.setFocusType(weaknessBlock.getFocusType());
                if (includes.has("weaknessblocks.questiontype")) weaknessBlockDto.setQuestionType(weaknessBlock.getQuestionType());
                if (includes.has("weaknessblocks.topictag")) weaknessBlockDto.setTopicTag(weaknessBlock.getTopicTag());
                if (includes.has("weaknessblocks.status")) weaknessBlockDto.setStatus(weaknessBlock.getStatus());
                if (includes.has("weaknessblocks.analyticsconclusionlabel")) {
                    weaknessBlockDto.setAnalyticsConclusionLabel(weaknessBlock.getAnalyticsConclusionLabel());
                }
                if (includes.has("weaknessblocks.description")) weaknessBlockDto.setDescription(weaknessBlock.getDescription());
                if (includes.has("weaknessblocks.explanation")) weaknessBlockDto.setExplanation(weaknessBlock.getExplanation());
                if (includes.has("weaknessblocks.evidence")) weaknessBlockDto.setEvidence(weaknessBlock.getEvidence());
                if (includes.has("weaknessblocks.recommendednextaction")) {
                    weaknessBlockDto.setRecommendedNextAction(weaknessBlock.getRecommendedNextAction());
                }

                weaknessBlockDtos.add(weaknessBlockDto);
            }

            dto.setWeaknessBlocks(weaknessBlockDtos);
        }
    }

    private void attachTestingSubmissions(LearnerStudyPlan studyPlan, List<UserPracticeSubmission> submissions) {
        int order = 1;
        for (UserPracticeSubmission submission : submissions) {
            submission.setLearnerStudyPlan(studyPlan);
            studyPlan.getSubmissions().add(submission);
        }
    }

    private void generateStrengthBlocks(
            LearnerStudyPlan studyPlan,
            LearnerAnalyticsSnapshot analyticsSnapshot,
            LearnerTrendSnapshot trendSnapshot
    ) {
        List<FocusCandidate> strongQuestionTypeCandidates = buildQuestionTypeCandidates(analyticsSnapshot, trendSnapshot).stream()
                .filter(FocusCandidate::isEligibleForRanking)
                .filter(candidate -> candidate.getStrengthLabel() == AnalyticsStrengthLabel.STRONG)
                .sorted(
                        Comparator.comparing(FocusCandidate::getRollingCorrectAnswerPercentage).reversed()
                                .thenComparing(FocusCandidate::getRollingEffectiveAccuracy, Comparator.reverseOrder())
                                .thenComparing(FocusCandidate::getRollingSkipRate)
                )
                .limit(MAX_STRENGTH_QUESTION_TYPES)
                .toList();

        List<FocusCandidate> strongTopicCandidates = buildTopicCandidates(analyticsSnapshot, trendSnapshot).stream()
                .filter(FocusCandidate::isEligibleForRanking)
                .filter(candidate -> candidate.getStrengthLabel() == AnalyticsStrengthLabel.STRONG)
                .sorted(
                        Comparator.comparing(FocusCandidate::getRollingCorrectAnswerPercentage).reversed()
                                .thenComparing(FocusCandidate::getRollingEffectiveAccuracy, Comparator.reverseOrder())
                                .thenComparing(FocusCandidate::getRollingSkipRate)
                )
                .limit(MAX_STRENGTH_TOPICS)
                .toList();

        List<FocusCandidate> selected = new ArrayList<>();
        selected.addAll(strongQuestionTypeCandidates);
        selected.addAll(strongTopicCandidates);

        selected = selected.stream()
                .sorted(
                        Comparator.comparing(FocusCandidate::getRollingCorrectAnswerPercentage).reversed()
                                .thenComparing(FocusCandidate::getRollingEffectiveAccuracy, Comparator.reverseOrder())
                                .thenComparing(FocusCandidate::getRollingSkipRate)
                )
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
            block.setStatus(resolveStrengthBlockStatus(candidate.getAccuracyTrendLabel()));
            block.setAnalyticsConclusionLabel(candidate.getConclusionLabel());

            studyPlan.getStrengthBlocks().add(block);
        }
    }

    private void generateWeaknessBlocks(
            LearnerStudyPlan studyPlan,
            LearnerAnalyticsSnapshot analyticsSnapshot,
            LearnerTrendSnapshot trendSnapshot
    ) {
        List<FocusCandidate> weakQuestionTypeCandidates = buildQuestionTypeCandidates(analyticsSnapshot, trendSnapshot).stream()
                .filter(FocusCandidate::isEligibleForRanking)
                .filter(candidate -> candidate.getStrengthLabel() == AnalyticsStrengthLabel.WEAK)
                .sorted(weaknessComparator())
                .limit(MAX_WEAKNESS_QUESTION_TYPES)
                .toList();

        List<FocusCandidate> weakTopicCandidates = buildTopicCandidates(analyticsSnapshot, trendSnapshot).stream()
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
            block.setStatus(resolveWeaknessBlockStatus(candidate.getAccuracyTrendLabel()));
            block.setAnalyticsConclusionLabel(candidate.getConclusionLabel());

            studyPlan.getWeaknessBlocks().add(block);
        }
    }

    private void generateTasks(
            LearnerStudyPlan studyPlan,
            LearnerAnalyticsSnapshot analyticsSnapshot,
            LearnerTrendSnapshot trendSnapshot
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
            LearnerStudyPlanTargetMetric targetMetric = resolveTargetMetric(candidate.getConclusionLabel());
            LearnerStudyPlanTaskDirection direction = resolveTaskDirection(targetMetric);
            double currentValue = resolveCurrentValue(candidate, targetMetric);
            double targetValue = resolveTargetValue(currentValue, direction, candidate.getStrengthLabel());

            LearnerStudyPlanTask task = new LearnerStudyPlanTask();
            task.setLearnerStudyPlan(studyPlan);
            task.setPriorityRank(rank++);
            task.setFocusType(candidate.getFocusType());
            task.setQuestionType(candidate.getQuestionType());
            task.setTopicTag(candidate.getTopicTag());
            task.setTargetMetric(targetMetric);
            task.setCurrentValue(currentValue);
            task.setTargetValue(targetValue);
            task.setDirection(direction);
            task.setStatus(LearnerStudyPlanTaskStatus.ACTIVE);

            studyPlan.getTasks().add(task);
        }
    }

    private List<FocusCandidate> buildAllCandidates(
            LearnerAnalyticsSnapshot analyticsSnapshot,
            LearnerTrendSnapshot trendSnapshot
    ) {
        List<FocusCandidate> candidates = new ArrayList<>();
        candidates.addAll(buildQuestionTypeCandidates(analyticsSnapshot, trendSnapshot));
        candidates.addAll(buildTopicCandidates(analyticsSnapshot, trendSnapshot));
        return candidates;
    }

    private List<FocusCandidate> buildQuestionTypeCandidates(
            LearnerAnalyticsSnapshot analyticsSnapshot,
            LearnerTrendSnapshot trendSnapshot
    ) {
        Map<PracticeQuestionType, LearnerQuestionTypeTrend> trendMap =
                trendSnapshot.getQuestionTypeTrends().stream()
                        .collect(Collectors.toMap(LearnerQuestionTypeTrend::getQuestionType, Function.identity()));

        return analyticsSnapshot.getQuestionTypeAnalytics().stream()
                .map(analytics -> toQuestionTypeCandidate(analytics, trendMap.get(analytics.getQuestionType())))
                .toList();
    }

    private List<FocusCandidate> buildTopicCandidates(
            LearnerAnalyticsSnapshot analyticsSnapshot,
            LearnerTrendSnapshot trendSnapshot
    ) {
        Map<PracticeTopicTag, LearnerTopicTagTrend> trendMap =
                trendSnapshot.getTopicTagTrends().stream()
                        .collect(Collectors.toMap(LearnerTopicTagTrend::getTopicTag, Function.identity()));

        return analyticsSnapshot.getTopicTagAnalytics().stream()
                .map(analytics -> toTopicCandidate(analytics, trendMap.get(analytics.getTopicTag())))
                .toList();
    }

    private FocusCandidate toQuestionTypeCandidate(
            LearnerQuestionTypeAnalytics analytics,
            LearnerQuestionTypeTrend trend
    ) {
        return new FocusCandidate(
                LearnerStudyPlanFocusType.QUESTION_TYPE,
                analytics.getQuestionType(),
                null,
                analytics.getContributedSubmissionCount(),
                analytics.getRollingExposureCount(),
                analytics.getRollingCorrectAnswerPercentage(),
                analytics.getRollingSkipRate(),
                analytics.getRollingEffectiveAccuracy(),
                analytics.getStrengthLabel(),
                analytics.getConclusionLabel(),
                trend != null ? trend.getAccuracySlope() : 0.0,
                trend != null ? trend.getAccuracyTrendLabel() : TrendLabel.STABLE
        );
    }

    private FocusCandidate toTopicCandidate(
            LearnerTopicTagAnalytics analytics,
            LearnerTopicTagTrend trend
    ) {
        return new FocusCandidate(
                LearnerStudyPlanFocusType.TOPIC,
                null,
                analytics.getTopicTag(),
                analytics.getContributedSubmissionCount(),
                analytics.getRollingExposureCount(),
                analytics.getRollingCorrectAnswerPercentage(),
                analytics.getRollingSkipRate(),
                analytics.getRollingEffectiveAccuracy(),
                analytics.getStrengthLabel(),
                analytics.getConclusionLabel(),
                trend != null ? trend.getAccuracySlope() : 0.0,
                trend != null ? trend.getAccuracyTrendLabel() : TrendLabel.STABLE
        );
    }

    private Comparator<FocusCandidate> weaknessComparator() {
        return Comparator.comparing(FocusCandidate::getRollingCorrectAnswerPercentage)
                .thenComparing(FocusCandidate::getRollingSkipRate, Comparator.reverseOrder())
                .thenComparing(FocusCandidate::getRollingEffectiveAccuracy);
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

    private LearnerStudyPlanTargetMetric resolveTargetMetric(AnalyticsConclusionLabel conclusionLabel) {
        return switch (conclusionLabel) {
            case HESITANT, AVOIDANT -> LearnerStudyPlanTargetMetric.ROLLING_SKIP_RATE;
            case ERROR_PRONE -> LearnerStudyPlanTargetMetric.ROLLING_CORRECT_ANSWER_PERCENTAGE;
            case DEVELOPING, MIXED, BALANCED, MASTERED -> LearnerStudyPlanTargetMetric.ROLLING_EFFECTIVE_ACCURACY;
        };
    }

    private LearnerStudyPlanTaskDirection resolveTaskDirection(LearnerStudyPlanTargetMetric metric) {
        return switch (metric) {
            case ROLLING_SKIP_RATE -> LearnerStudyPlanTaskDirection.REDUCE;
            case ROLLING_CORRECT_ANSWER_PERCENTAGE,
                 ROLLING_EFFECTIVE_ACCURACY -> LearnerStudyPlanTaskDirection.INCREASE;
            case ROLLING_OVERALL_BAND_SCORE -> throw new IllegalArgumentException(
                    "ROLLING_OVERALL_BAND_SCORE is only supported for Writing study plans"
            );
        };
    }

    private double resolveCurrentValue(FocusCandidate candidate, LearnerStudyPlanTargetMetric metric) {
        return switch (metric) {
            case ROLLING_CORRECT_ANSWER_PERCENTAGE -> candidate.getRollingCorrectAnswerPercentage();
            case ROLLING_SKIP_RATE -> candidate.getRollingSkipRate();
            case ROLLING_EFFECTIVE_ACCURACY -> candidate.getRollingEffectiveAccuracy();
            case ROLLING_OVERALL_BAND_SCORE -> throw new IllegalArgumentException(
                    "ROLLING_OVERALL_BAND_SCORE is only supported for Writing study plans"
            );
        };
    }

    private double resolveTargetValue(
            double currentValue,
            LearnerStudyPlanTaskDirection direction,
            AnalyticsStrengthLabel strengthLabel
    ) {
        double step = switch (strengthLabel) {
            case WEAK -> WEAK_TARGET_STEP;
            case NEUTRAL -> NEUTRAL_TARGET_STEP;
            case STRONG -> STRONG_TARGET_STEP;
        };

        if (direction == LearnerStudyPlanTaskDirection.REDUCE) {
            return Math.max(0.0, currentValue - step);
        }

        return currentValue + step;
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

    @Getter
    @AllArgsConstructor
    private static class FocusCandidate {
        private LearnerStudyPlanFocusType focusType;
        private PracticeQuestionType questionType;
        private PracticeTopicTag topicTag;
        private Integer contributedSubmissionCount;
        private Integer rollingExposureCount;
        private Double rollingCorrectAnswerPercentage;
        private Double rollingSkipRate;
        private Double rollingEffectiveAccuracy;
        private AnalyticsStrengthLabel strengthLabel;
        private AnalyticsConclusionLabel conclusionLabel;
        private Double accuracySlope;
        private TrendLabel accuracyTrendLabel;

        boolean isEligibleForRanking() {
            return rollingExposureCount != null
                    && contributedSubmissionCount != null
                    && rollingExposureCount >= MIN_EXPOSURE_COUNT
                    && contributedSubmissionCount >= MIN_SUBMISSION_COVERAGE;
        }
    }

    private void refreshTaskProgress(
            LearnerStudyPlan studyPlan,
            String userId,
            PracticeContentSkill skill
    ) {
        for (LearnerStudyPlanTask task : studyPlan.getTasks()) {
            Double currentValue = learnerAnalyticsSnapshotService.getCurrentValue(
                    task.getFocusType(),
                    task.getTargetMetric(),
                    userId,
                    skill,
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
                || submissionCountSinceCreation < MIN_SUBMISSION_COUNT_TO_FINALIZE) {
            return false;
        }

        long completedTaskCount = studyPlan.getTasks().stream()
                .filter(task -> task.getStatus() == LearnerStudyPlanTaskStatus.COMPLETED)
                .count();

        int totalTaskCount = studyPlan.getTasks().size();

        double completedTaskPercentage = (double) completedTaskCount * 100.0 / totalTaskCount;

        return completedTaskPercentage >= MIN_COMPLETED_TASK_PERCENTAGE_TO_FINALIZE;
    }
}
