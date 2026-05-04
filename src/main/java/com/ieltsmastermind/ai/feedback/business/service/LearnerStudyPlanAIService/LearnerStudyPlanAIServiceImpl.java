package com.ieltsmastermind.ai.feedback.business.service.LearnerStudyPlanAIService;

import com.ieltsmastermind.ai.feedback.business.interfaces.LearnerStudyPlanAIService;
import com.ieltsmastermind.ai.feedback.business.service.LearnerStudyPlanAIService.LearnerStudyPlanAIUpdater;
import com.ieltsmastermind.ai.feedback.business.service.LearnerStudyPlanAIService.OpenAIClient;
import com.ieltsmastermind.ai.feedback.domain.dto.StudyPlanAIResponse;
import com.ieltsmastermind.ai.feedback.domain.dto.TaskInput;
import com.ieltsmastermind.ai.feedback.domain.entity.AIInput;
import com.ieltsmastermind.practice.analytics.management.domain.entity.FocusTypeAnalytics;
import com.ieltsmastermind.practice.analytics.management.domain.entity.SubmissionAnalytics;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTopicTag;
import com.ieltsmastermind.practice.studyplan.management.domain.entity.LearnerStudyPlan;
import com.ieltsmastermind.practice.studyplan.management.domain.entity.LearnerStudyPlanStrengthBlock;
import com.ieltsmastermind.practice.studyplan.management.domain.entity.LearnerStudyPlanTask;
import com.ieltsmastermind.practice.studyplan.management.domain.entity.LearnerStudyPlanWeaknessBlock;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanFocusType;
import com.ieltsmastermind.practice.studyplan.management.persistence.LearnerStudyPlanRepository;
import com.ieltsmastermind.practice.studyplan.management.persistence.LearnerStudyPlanStrengthBlockRepository;
import com.ieltsmastermind.practice.studyplan.management.persistence.LearnerStudyPlanTaskRepository;
import com.ieltsmastermind.practice.studyplan.management.persistence.LearnerStudyPlanWeaknessBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearnerStudyPlanAIServiceImpl implements LearnerStudyPlanAIService {

    private final OpenAIClient aiClient;
    private final LearnerStudyPlanRepository studyPlanRepository;
    private final LearnerStudyPlanTaskRepository taskRepository;
    private final LearnerStudyPlanStrengthBlockRepository strengthRepository;
    private final LearnerStudyPlanWeaknessBlockRepository weaknessRepository;
    private final LearnerStudyPlanAIUpdater updater;

    @Override
    @Transactional
    public void triggerAIContentGeneration(String studyPlanId) {

        // 1. Load study plan
        LearnerStudyPlan studyPlan = studyPlanRepository.findById(studyPlanId)
                .orElseThrow(() -> new RuntimeException("Study plan not found"));



        SubmissionAnalytics submissionAnalytics = Optional.ofNullable(studyPlan.getSubmissionAnalytics())
                .orElseThrow(() -> new RuntimeException("Submission analytics not found"));

        boolean isWriting = studyPlan.getSkill() == PracticeContentSkill.WRITING;



        // 2. Load blocks
        List<LearnerStudyPlanWeaknessBlock> weaknesses =
                weaknessRepository.findWeaknessBlocksByStudyPlanId(studyPlanId);

        List<LearnerStudyPlanStrengthBlock> strengths =
                strengthRepository.findStrengthBlocksByStudyPlanId(studyPlanId);

        // 3. Extract focus
        Set<PracticeQuestionType> questionTypes = new HashSet<>();
        Set<PracticeTopicTag> topicTags = new HashSet<>();

        extractFocusFromWeakness(weaknesses, questionTypes, topicTags);
        extractFocusFromStrength(strengths, questionTypes, topicTags);

        // 4. Load analytics from FocusTypeAnalytics
        Map<PracticeQuestionType, FocusTypeAnalytics> questionAnalyticsMap =
                buildQuestionTypeAnalyticsMap(submissionAnalytics, questionTypes);

        Map<PracticeTopicTag, FocusTypeAnalytics> topicAnalyticsMap =
                buildTopicTagAnalyticsMap(submissionAnalytics, topicTags);

        // 5. Build AI input
        List<AIInput> inputs = buildAIInputs(
                weaknesses,
                strengths,
                questionAnalyticsMap,
                topicAnalyticsMap,
                isWriting
        );

        List<AIInput> weaknessInputs = inputs.stream()
                .filter(i -> "WEAKNESS".equals(i.getType()))
                .toList();

        List<AIInput> strengthInputs = inputs.stream()
                .filter(i -> "STRENGTH".equals(i.getType()))
                .toList();

        // Tasks must be built separately
        List<TaskInput> taskInputs = buildTaskInputs(studyPlanId);

        // 6. Call AI batch generation
        StudyPlanAIResponse response = aiClient.generateWithRetry(
                weaknessInputs,
                strengthInputs,
                taskInputs,
                isWriting
        );


        updater.saveAIResult(studyPlanId, response);
    }

    private Map<PracticeQuestionType, FocusTypeAnalytics> buildQuestionTypeAnalyticsMap(
            SubmissionAnalytics submissionAnalytics,
            Set<PracticeQuestionType> questionTypes
    ) {
        if (questionTypes.isEmpty()) {
            return Collections.emptyMap();
        }

        return submissionAnalytics.getFocusTypeAnalytics()
                .stream()
                .filter(analytics -> analytics.getFocusType() == LearnerStudyPlanFocusType.QUESTION_TYPE)
                .filter(analytics -> analytics.getQuestionType() != null)
                .filter(analytics -> questionTypes.contains(analytics.getQuestionType()))
                .collect(Collectors.toMap(
                        FocusTypeAnalytics::getQuestionType,
                        Function.identity(),
                        (existing, replacement) -> existing,
                        HashMap::new
                ));
    }

    private Map<PracticeTopicTag, FocusTypeAnalytics> buildTopicTagAnalyticsMap(
            SubmissionAnalytics submissionAnalytics,
            Set<PracticeTopicTag> topicTags
    ) {
        if (topicTags.isEmpty()) {
            return Collections.emptyMap();
        }

        return submissionAnalytics.getFocusTypeAnalytics()
                .stream()
                .filter(analytics -> analytics.getFocusType() != LearnerStudyPlanFocusType.QUESTION_TYPE)
                .filter(analytics -> analytics.getTopicTag() != null)
                .filter(analytics -> topicTags.contains(analytics.getTopicTag()))
                .collect(Collectors.toMap(
                        FocusTypeAnalytics::getTopicTag,
                        Function.identity(),
                        (existing, replacement) -> existing,
                        HashMap::new
                ));
    }

    private void extractFocusFromWeakness(
            List<LearnerStudyPlanWeaknessBlock> blocks,
            Set<PracticeQuestionType> questionTypes,
            Set<PracticeTopicTag> topicTags
    ) {
        for (LearnerStudyPlanWeaknessBlock block : blocks) {
            if (block.getFocusType() == LearnerStudyPlanFocusType.QUESTION_TYPE) {
                if (block.getQuestionType() != null) {
                    questionTypes.add(block.getQuestionType());
                }
            } else {
                if (block.getTopicTag() != null) {
                    topicTags.add(block.getTopicTag());
                }
            }
        }
    }

    private void extractFocusFromStrength(
            List<LearnerStudyPlanStrengthBlock> blocks,
            Set<PracticeQuestionType> questionTypes,
            Set<PracticeTopicTag> topicTags
    ) {
        for (LearnerStudyPlanStrengthBlock block : blocks) {
            if (block.getFocusType() == LearnerStudyPlanFocusType.QUESTION_TYPE) {
                if (block.getQuestionType() != null) {
                    questionTypes.add(block.getQuestionType());
                }
            } else {
                if (block.getTopicTag() != null) {
                    topicTags.add(block.getTopicTag());
                }
            }
        }
    }

    private List<AIInput> buildAIInputs(
            List<LearnerStudyPlanWeaknessBlock> weaknesses,
            List<LearnerStudyPlanStrengthBlock> strengths,
            Map<PracticeQuestionType, FocusTypeAnalytics> questionAnalyticsMap,
            Map<PracticeTopicTag, FocusTypeAnalytics> topicAnalyticsMap,
            boolean isWriting
    ) {
        List<AIInput> inputs = new ArrayList<>();

        for (LearnerStudyPlanWeaknessBlock weakness : weaknesses) {
            inputs.add(buildWeaknessInput(
                    weakness,
                    questionAnalyticsMap,
                    topicAnalyticsMap,
                    isWriting
            ));
        }

        for (LearnerStudyPlanStrengthBlock strength : strengths) {
            inputs.add(buildStrengthInput(
                    strength,
                    questionAnalyticsMap,
                    topicAnalyticsMap,
                    isWriting
            ));
        }

        return inputs;
    }

    private List<TaskInput> buildTaskInputs(String studyPlanId) {
        List<LearnerStudyPlanTask> tasks =
                taskRepository.findTasksByStudyPlanId(studyPlanId);

        List<TaskInput> inputs = new ArrayList<>();

        for (LearnerStudyPlanTask task : tasks) {
            inputs.add(new TaskInput(
                    task.getId(),
                    task.getFocusType(),
                    task.getQuestionType(),
                    task.getTopicTag(),
                    task.getDirection()
            ));
        }

        return inputs;
    }

    private AIInput buildWeaknessInput(
            LearnerStudyPlanWeaknessBlock block,
            Map<PracticeQuestionType, FocusTypeAnalytics> questionAnalyticsMap,
            Map<PracticeTopicTag, FocusTypeAnalytics> topicAnalyticsMap,
            boolean isWriting
    ) {
        return buildInputCommon(
                block.getId(),
                "WEAKNESS",
                block.getFocusType(),
                block.getQuestionType(),
                block.getTopicTag(),
                questionAnalyticsMap,
                topicAnalyticsMap,
                isWriting
        );
    }

    private AIInput buildStrengthInput(
            LearnerStudyPlanStrengthBlock block,
            Map<PracticeQuestionType, FocusTypeAnalytics> questionAnalyticsMap,
            Map<PracticeTopicTag, FocusTypeAnalytics> topicAnalyticsMap,
            boolean isWriting
    ) {
        return buildInputCommon(
                block.getId(),
                "STRENGTH",
                block.getFocusType(),
                block.getQuestionType(),
                block.getTopicTag(),
                questionAnalyticsMap,
                topicAnalyticsMap,
                isWriting
        );
    }

    private AIInput buildInputCommon(
            String id,
            String type,
            LearnerStudyPlanFocusType focusType,
            PracticeQuestionType questionType,
            PracticeTopicTag topicTag,
            Map<PracticeQuestionType, FocusTypeAnalytics> questionAnalyticsMap,
            Map<PracticeTopicTag, FocusTypeAnalytics> topicAnalyticsMap,
            boolean isWriting
    ) {
        double metricValue = 0.0;

        FocusTypeAnalytics analytics;

        if (focusType == LearnerStudyPlanFocusType.QUESTION_TYPE) {
            analytics = questionAnalyticsMap.get(questionType);
        } else {
            analytics = topicAnalyticsMap.get(topicTag);
        }

        if (analytics != null) {
            if (isWriting) {
                metricValue = safeDouble(
                        analytics.getRollingOverallBandScore()
                );
            } else {
                metricValue = safeDouble(
                        analytics.getRollingCorrectAnswerPercentage()
                );
            }
        }

        return new AIInput(
                id,
                type,
                focusType,
                questionType,
                topicTag,
                metricValue
        );
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }
}