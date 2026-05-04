package com.ieltsmastermind.practice.studyplan.management.business.service;

import com.ieltsmastermind.ai.feedback.business.interfaces.LearnerStudyPlanAIService;
import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.analytics.management.business.interfaces.SubmissionAnalyticsService;
import com.ieltsmastermind.practice.analytics.management.domain.entity.FocusTypeAnalytics;
import com.ieltsmastermind.practice.analytics.management.domain.entity.SubmissionAnalytics;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTopicTag;
import com.ieltsmastermind.practice.studyplan.management.business.interfaces.LearnerStudyPlanService;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class LearnerStudyPlanServiceImpl implements LearnerStudyPlanService {

    private static final int MAX_STRENGTH_BLOCKS = 4;
    private static final int MAX_WEAKNESS_BLOCKS = 4;
    private static final int MAX_WEAKNESS_TASKS = 4;

    private static final int MIN_SUBMISSION_COUNT_REQUIRED = 5;
    private static final int MIN_SUBMISSION_COUNT_TO_FINALIZE = 5;
    private static final double MIN_COMPLETED_TASK_PERCENTAGE_TO_FINALIZE = 75.0;

    private static final double CORRECT_ANSWER_TARGET_STEP = 5.0;
    private static final double BAND_SCORE_TARGET_STEP = 0.5;
    private static final double MAX_CORRECT_ANSWER_PERCENTAGE = 100.0;
    private static final double MAX_BAND_SCORE = 9.0;

    private final LearnerStudyPlanRepository learnerStudyPlanRepository;
    private final UserRepository userRepository;
    private final SubmissionAnalyticsService submissionAnalyticsService;

    private final LearnerStudyPlanAIService aiService;

    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public LearnerStudyPlanResponseDto create(LearnerStudyPlanCreateRequestDto request) {
        User learner = userRepository.findById(request.getLearnerId())
                .orElseThrow(() -> new IllegalArgumentException("Learner not found"));

        PracticeContentSkill skill = request.getSkill();

        SubmissionAnalytics submissionAnalytics =
                submissionAnalyticsService.createSnapshot(learner.getUserId(), skill);

        if (submissionAnalytics.getSubmissionCountUsed() < MIN_SUBMISSION_COUNT_REQUIRED) {
            throw new IllegalArgumentException(
                    "At least 5 submissions are required for learner and skill"
            );
        }

        LearnerStudyPlan latestStudyPlan = learnerStudyPlanRepository
                .findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(
                        learner.getUserId(),
                        skill
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
        studyPlan.setSkill(skill);
        studyPlan.setVersionNumber(nextVersionNumber);
        studyPlan.setStatus(LearnerStudyPlanStatus.ACTIVE);
        studyPlan.setSubmissionCountUsed(submissionAnalytics.getSubmissionCountUsed());
        studyPlan.setSubmissionCountSinceCreation(0);
        studyPlan.setSubmissionAnalytics(submissionAnalytics);

        generateStrengthBlocks(studyPlan, submissionAnalytics);
        generateWeaknessBlocks(studyPlan, submissionAnalytics);
        generateTasks(studyPlan, submissionAnalytics);

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
        aiService.triggerAIContentGeneration(event.getStudyPlanId());
    }

    @Override
    @Transactional
    public LearnerStudyPlanResponseDto getActiveStudyPlanByUserIdAndSkill(
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
        dto.setId(studyPlan.getId());

        applyIncludes(studyPlan, dto, includes);

        return dto;
    }

    @Override
    @Transactional
    public LearnerStudyPlanActiveCheckResponseDto getHasActiveStudyPlan(
            String userId,
            PracticeContentSkill skill
    ) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Learner not found"));

        boolean hasActiveStudyPlan =
                learnerStudyPlanRepository.existsByUser_UserIdAndSkillAndStatus(
                        userId,
                        skill,
                        LearnerStudyPlanStatus.ACTIVE
                );

        LearnerStudyPlanActiveCheckResponseDto responseDto =
                new LearnerStudyPlanActiveCheckResponseDto();

        responseDto.setHasActiveStudyPlan(hasActiveStudyPlan);

        return responseDto;
    }

    @Override
    @Transactional
    public LearnerStudyPlanResponseDto refreshStudyPlan(
            String userId,
            PracticeContentSkill skill
    ) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Learner not found"));

        LearnerStudyPlan studyPlan = learnerStudyPlanRepository
                .findTopByUser_UserIdAndSkillAndStatusOrderByVersionNumberDesc(
                        userId,
                        skill,
                        LearnerStudyPlanStatus.ACTIVE
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Active learner study plan not found"
                ));

        SubmissionAnalytics submissionAnalytics =
                submissionAnalyticsService.createSnapshot(userId, skill);

        int submissionCountSinceCreation =
                resolveSubmissionCountSinceCreation(submissionAnalytics, studyPlan);
        studyPlan.setSubmissionCountSinceCreation(submissionCountSinceCreation);

        refreshTaskProgress(studyPlan, userId, skill);

        studyPlan.setReadyToFinalize(resolveStudyPlanReadyToFinalize(studyPlan));

        LearnerStudyPlan saved = learnerStudyPlanRepository.save(studyPlan);

        LearnerStudyPlanResponseDto responseDto =
                new LearnerStudyPlanResponseDto();

        responseDto.setId(saved.getId());

        return responseDto;
    }

    @Override
    @Transactional
    public LearnerStudyPlanResponseDto finalizeStudyPlanById(String id) {
        LearnerStudyPlan studyPlan = learnerStudyPlanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Learner study plan not found"));

        studyPlan.setStatus(LearnerStudyPlanStatus.INACTIVE);

        LearnerStudyPlan saved = learnerStudyPlanRepository.save(studyPlan);

        LearnerStudyPlanResponseDto responseDto = new LearnerStudyPlanResponseDto();
        responseDto.setId(saved.getId());

        return responseDto;
    }

    private void generateStrengthBlocks(
            LearnerStudyPlan studyPlan,
            SubmissionAnalytics submissionAnalytics
    ) {
        List<FocusCandidate> selected = buildAllCandidates(submissionAnalytics)
                .stream()
                .sorted(strengthComparator(submissionAnalytics.getSkill()))
                .limit(MAX_STRENGTH_BLOCKS)
                .toList();

        int rank = 1;

        for (FocusCandidate candidate : selected) {
            LearnerStudyPlanStrengthBlock block =
                    new LearnerStudyPlanStrengthBlock();

            block.setLearnerStudyPlan(studyPlan);
            block.setStrengthRank(rank++);
            block.setFocusType(candidate.getFocusType());
            block.setQuestionType(candidate.getQuestionType());
            block.setTopicTag(candidate.getTopicTag());

            studyPlan.getStrengthBlocks().add(block);
        }
    }

    private void generateWeaknessBlocks(
            LearnerStudyPlan studyPlan,
            SubmissionAnalytics submissionAnalytics
    ) {
        List<FocusCandidate> selected = buildAllCandidates(submissionAnalytics)
                .stream()
                .sorted(weaknessComparator(submissionAnalytics.getSkill()))
                .limit(MAX_WEAKNESS_BLOCKS)
                .toList();

        int rank = 1;

        for (FocusCandidate candidate : selected) {
            LearnerStudyPlanWeaknessBlock block =
                    new LearnerStudyPlanWeaknessBlock();

            block.setLearnerStudyPlan(studyPlan);
            block.setWeaknessRank(rank++);
            block.setFocusType(candidate.getFocusType());
            block.setQuestionType(candidate.getQuestionType());
            block.setTopicTag(candidate.getTopicTag());

            studyPlan.getWeaknessBlocks().add(block);
        }
    }

    private void generateTasks(
            LearnerStudyPlan studyPlan,
            SubmissionAnalytics submissionAnalytics
    ) {
        List<FocusCandidate> taskCandidates = buildAllCandidates(submissionAnalytics)
                .stream()
                .sorted(weaknessComparator(submissionAnalytics.getSkill()))
                .limit(MAX_WEAKNESS_TASKS)
                .toList();

        int rank = 1;

        for (FocusCandidate candidate : taskCandidates) {
            boolean writing = submissionAnalytics.getSkill() == PracticeContentSkill.WRITING;

            LearnerStudyPlanTargetMetric targetMetric = writing
                    ? LearnerStudyPlanTargetMetric.ROLLING_OVERALL_BAND_SCORE
                    : LearnerStudyPlanTargetMetric.ROLLING_CORRECT_ANSWER_PERCENTAGE;

            double currentValue = writing
                    ? safeDouble(candidate.getRollingOverallBandScore())
                    : safeDouble(candidate.getRollingCorrectAnswerPercentage());

            double targetValue = resolveTargetValue(currentValue, writing);

            LearnerStudyPlanTask task = new LearnerStudyPlanTask();

            task.setLearnerStudyPlan(studyPlan);
            task.setPriorityRank(rank++);
            task.setFocusType(candidate.getFocusType());
            task.setQuestionType(candidate.getQuestionType());
            task.setTopicTag(candidate.getTopicTag());
            task.setTargetMetric(targetMetric);
            task.setCurrentValue(currentValue);
            task.setTargetValue(targetValue);
            task.setDirection(LearnerStudyPlanTaskDirection.INCREASE);
            task.setStatus(LearnerStudyPlanTaskStatus.ACTIVE);

            studyPlan.getTasks().add(task);
        }
    }

    private List<FocusCandidate> buildAllCandidates(
            SubmissionAnalytics submissionAnalytics
    ) {
        return submissionAnalytics.getFocusTypeAnalytics()
                .stream()
                .map(this::toFocusCandidate)
                .toList();
    }

    private FocusCandidate toFocusCandidate(FocusTypeAnalytics analytics) {
        return new FocusCandidate(
                analytics.getFocusType(),
                analytics.getQuestionType(),
                analytics.getTopicTag(),
                analytics.getContributedSubmissionCount(),
                analytics.getRollingExposureCount(),
                analytics.getRollingCorrectAnswerPercentage(),
                analytics.getRollingOverallBandScore()
        );
    }

    private Comparator<FocusCandidate> weaknessComparator(
            PracticeContentSkill skill
    ) {
        if (skill == PracticeContentSkill.WRITING) {
            return Comparator.comparing(
                    FocusCandidate::getRollingOverallBandScore,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
        }

        return Comparator.comparing(
                FocusCandidate::getRollingCorrectAnswerPercentage,
                Comparator.nullsLast(Comparator.naturalOrder())
        );
    }

    private Comparator<FocusCandidate> strengthComparator(
            PracticeContentSkill skill
    ) {
        if (skill == PracticeContentSkill.WRITING) {
            return Comparator.comparing(
                    FocusCandidate::getRollingOverallBandScore,
                    Comparator.nullsLast(Comparator.reverseOrder())
            );
        }

        return Comparator.comparing(
                FocusCandidate::getRollingCorrectAnswerPercentage,
                Comparator.nullsLast(Comparator.reverseOrder())
        );
    }

    private double resolveTargetValue(
            double currentValue,
            boolean writing
    ) {
        if (writing) {
            return Math.min(MAX_BAND_SCORE, currentValue + BAND_SCORE_TARGET_STEP);
        }

        return Math.min(
                MAX_CORRECT_ANSWER_PERCENTAGE,
                currentValue + CORRECT_ANSWER_TARGET_STEP
        );
    }

    private int resolveSubmissionCountSinceCreation(
            SubmissionAnalytics submissionAnalytics,
            LearnerStudyPlan studyPlan
    ) {
        int analyticsSubmissionCountUsed =
                submissionAnalytics.getSubmissionCountUsed() != null
                        ? submissionAnalytics.getSubmissionCountUsed()
                        : 0;

        int studyPlanSubmissionCountUsed =
                studyPlan.getSubmissionCountUsed() != null
                        ? studyPlan.getSubmissionCountUsed()
                        : 0;

        return Math.max(
                0,
                analyticsSubmissionCountUsed - studyPlanSubmissionCountUsed
        );
    }

    private void refreshTaskProgress(
            LearnerStudyPlan studyPlan,
            String userId,
            PracticeContentSkill skill
    ) {
        for (LearnerStudyPlanTask task : studyPlan.getTasks()) {
            Double currentValue = submissionAnalyticsService.getCurrentValue(
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
        double targetValue = task.getTargetValue() != null
                ? task.getTargetValue()
                : 0.0;

        LearnerStudyPlanTaskDirection direction = task.getDirection();

        boolean completed = switch (direction) {
            case INCREASE -> currentValue >= targetValue;
            case REDUCE -> currentValue <= targetValue;
        };

        return completed
                ? LearnerStudyPlanTaskStatus.COMPLETED
                : LearnerStudyPlanTaskStatus.ACTIVE;
    }

    private boolean resolveStudyPlanReadyToFinalize(LearnerStudyPlan studyPlan) {
        if (studyPlan.getTasks() == null || studyPlan.getTasks().isEmpty()) {
            return false;
        }

        Integer submissionCountSinceCreation =
                studyPlan.getSubmissionCountSinceCreation();

        if (submissionCountSinceCreation == null
                || submissionCountSinceCreation < MIN_SUBMISSION_COUNT_TO_FINALIZE) {
            return false;
        }

        long completedTaskCount = studyPlan.getTasks()
                .stream()
                .filter(task -> task.getStatus() == LearnerStudyPlanTaskStatus.COMPLETED)
                .count();

        int totalTaskCount = studyPlan.getTasks().size();

        double completedTaskPercentage =
                (double) completedTaskCount * 100.0 / totalTaskCount;

        return completedTaskPercentage >= MIN_COMPLETED_TASK_PERCENTAGE_TO_FINALIZE;
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private void applyIncludes(
            LearnerStudyPlan studyPlan,
            LearnerStudyPlanResponseDto dto,
            IncludeSpec includes
    ) {
        if (includes.has("skill")) {
            dto.setSkill(studyPlan.getSkill());
        }

        if (includes.has("versionnumber")) {
            dto.setVersionNumber(studyPlan.getVersionNumber());
        }

        if (includes.has("status")) {
            dto.setStatus(studyPlan.getStatus());
        }

        if (includes.has("submissioncountused")) {
            dto.setSubmissionCountUsed(studyPlan.getSubmissionCountUsed());
        }

        if (includes.has("submissioncountsincecreation")) {
            dto.setSubmissionCountSinceCreation(
                    studyPlan.getSubmissionCountSinceCreation()
            );
        }

        if (includes.has("readytofinalize")) {
            dto.setReadyToFinalize(studyPlan.isReadyToFinalize());
        }

        boolean hasUserInclude =
                includes.has("user.userid")
                        || includes.has("user.email")
                        || includes.has("user.firstname")
                        || includes.has("user.lastname");

        if (hasUserInclude && studyPlan.getUser() != null) {
            LearnerStudyPlanUserResponseDto userDto =
                    new LearnerStudyPlanUserResponseDto();

            if (includes.has("user.userid")) {
                userDto.setUserId(studyPlan.getUser().getUserId());
            }

            if (includes.has("user.email")) {
                userDto.setEmail(studyPlan.getUser().getEmail());
            }

            if (includes.has("user.firstname")) {
                userDto.setFirstname(studyPlan.getUser().getFirstname());
            }

            if (includes.has("user.lastname")) {
                userDto.setLastname(studyPlan.getUser().getLastname());
            }

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

            for (LearnerStudyPlanTask task : studyPlan.getTasks()) {
                LearnerStudyPlanTaskResponseDto taskDto =
                        new LearnerStudyPlanTaskResponseDto();

                if (includes.has("tasks.id")) {
                    taskDto.setId(task.getId());
                }

                if (includes.has("tasks.focustype")) {
                    taskDto.setFocusType(task.getFocusType());
                }

                if (includes.has("tasks.questiontype")) {
                    taskDto.setQuestionType(task.getQuestionType());
                }

                if (includes.has("tasks.topictag")) {
                    taskDto.setTopicTag(task.getTopicTag());
                }

                if (includes.has("tasks.targetmetric")) {
                    taskDto.setTargetMetric(task.getTargetMetric());
                }

                if (includes.has("tasks.currentvalue")) {
                    taskDto.setCurrentValue(task.getCurrentValue());
                }

                if (includes.has("tasks.targetvalue")) {
                    taskDto.setTargetValue(task.getTargetValue());
                }

                if (includes.has("tasks.direction")) {
                    taskDto.setDirection(task.getDirection());
                }

                if (includes.has("tasks.status")) {
                    taskDto.setStatus(task.getStatus());
                }

                if (includes.has("tasks.description")) {
                    taskDto.setDescription(task.getDescription());
                }

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
            List<LearnerStudyPlanStrengthBlockResponseDto> strengthBlockDtos =
                    new ArrayList<>();

            for (LearnerStudyPlanStrengthBlock strengthBlock
                    : studyPlan.getStrengthBlocks()) {
                LearnerStudyPlanStrengthBlockResponseDto strengthBlockDto =
                        new LearnerStudyPlanStrengthBlockResponseDto();

                if (includes.has("strengthblocks.id")) {
                    strengthBlockDto.setId(strengthBlock.getId());
                }

                if (includes.has("strengthblocks.focustype")) {
                    strengthBlockDto.setFocusType(strengthBlock.getFocusType());
                }

                if (includes.has("strengthblocks.questiontype")) {
                    strengthBlockDto.setQuestionType(strengthBlock.getQuestionType());
                }

                if (includes.has("strengthblocks.topictag")) {
                    strengthBlockDto.setTopicTag(strengthBlock.getTopicTag());
                }

                if (includes.has("strengthblocks.description")) {
                    strengthBlockDto.setDescription(strengthBlock.getDescription());
                }

                if (includes.has("strengthblocks.explanation")) {
                    strengthBlockDto.setExplanation(strengthBlock.getExplanation());
                }

                if (includes.has("strengthblocks.evidence")) {
                    strengthBlockDto.setEvidence(strengthBlock.getEvidence());
                }

                if (includes.has("strengthblocks.recommendednextaction")) {
                    strengthBlockDto.setRecommendedNextAction(
                            strengthBlock.getRecommendedNextAction()
                    );
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
            List<LearnerStudyPlanWeaknessBlockResponseDto> weaknessBlockDtos =
                    new ArrayList<>();

            for (LearnerStudyPlanWeaknessBlock weaknessBlock
                    : studyPlan.getWeaknessBlocks()) {
                LearnerStudyPlanWeaknessBlockResponseDto weaknessBlockDto =
                        new LearnerStudyPlanWeaknessBlockResponseDto();

                if (includes.has("weaknessblocks.id")) {
                    weaknessBlockDto.setId(weaknessBlock.getId());
                }

                if (includes.has("weaknessblocks.focustype")) {
                    weaknessBlockDto.setFocusType(weaknessBlock.getFocusType());
                }

                if (includes.has("weaknessblocks.questiontype")) {
                    weaknessBlockDto.setQuestionType(weaknessBlock.getQuestionType());
                }

                if (includes.has("weaknessblocks.topictag")) {
                    weaknessBlockDto.setTopicTag(weaknessBlock.getTopicTag());
                }

                if (includes.has("weaknessblocks.description")) {
                    weaknessBlockDto.setDescription(weaknessBlock.getDescription());
                }

                if (includes.has("weaknessblocks.explanation")) {
                    weaknessBlockDto.setExplanation(weaknessBlock.getExplanation());
                }

                if (includes.has("weaknessblocks.evidence")) {
                    weaknessBlockDto.setEvidence(weaknessBlock.getEvidence());
                }

                if (includes.has("weaknessblocks.recommendednextaction")) {
                    weaknessBlockDto.setRecommendedNextAction(
                            weaknessBlock.getRecommendedNextAction()
                    );
                }

                weaknessBlockDtos.add(weaknessBlockDto);
            }

            dto.setWeaknessBlocks(weaknessBlockDtos);
        }
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

        private Double rollingOverallBandScore;
    }
}