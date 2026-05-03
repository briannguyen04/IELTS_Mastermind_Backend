package com.ieltsmastermind.ai.feedback.business.service.LearnerStudyPlanAIService;

import com.ieltsmastermind.ai.feedback.business.interfaces.LearnerStudyPlanAIService;
import com.ieltsmastermind.ai.feedback.domain.dto.StudyPlanAIResponse;
import com.ieltsmastermind.ai.feedback.domain.dto.TaskInput;
import com.ieltsmastermind.ai.feedback.domain.dto.TaskOutput;
import com.ieltsmastermind.ai.feedback.domain.entity.AIInput;
import com.ieltsmastermind.ai.feedback.domain.entity.AIOutput;
import com.ieltsmastermind.practice.analytics.management.domain.entity.LearnerQuestionTypeAnalytics;
import com.ieltsmastermind.practice.analytics.management.domain.entity.LearnerTopicTagAnalytics;
import com.ieltsmastermind.practice.analytics.management.persistence.LearnerQuestionTypeAnalyticsRepository;
import com.ieltsmastermind.practice.analytics.management.persistence.LearnerTopicTagAnalyticsRepository;
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
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;
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
    private final LearnerQuestionTypeAnalyticsRepository questionTypeAnalyticsRepository;
    private final LearnerTopicTagAnalyticsRepository topicTagAnalyticsRepository;
    private final LearnerStudyPlanAIUpdater updater;

    @Override
    public void triggerAIContentGeneration(String studyPlanId) {

        // 1. Load study plan
        LearnerStudyPlan studyPlan = studyPlanRepository.findById(studyPlanId)
                .orElseThrow();

        String snapshotId = Optional.ofNullable(studyPlan.getLearnerAnalyticsSnapshot())
                .map(s -> s.getId())
                .orElseThrow(() -> new RuntimeException("Snapshot not found"));

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

        List<PracticeQuestionType> questionTypeList = new ArrayList<>(questionTypes);
        List<PracticeTopicTag> topicTagList = new ArrayList<>(topicTags);

        // 4. Load analytics
        Map<PracticeQuestionType, LearnerQuestionTypeAnalytics> questionAnalyticsMap =
                questionTypeList.isEmpty()
                        ? Collections.emptyMap()
                        : questionTypeAnalyticsRepository
                        .findByLearnerAnalyticsSnapshot_IdAndQuestionTypeIn(snapshotId, questionTypeList)
                        .stream()
                        .collect(Collectors.toMap(
                                LearnerQuestionTypeAnalytics::getQuestionType,
                                Function.identity()
                        ));

        Map<PracticeTopicTag, LearnerTopicTagAnalytics> topicAnalyticsMap =
                topicTagList.isEmpty()
                        ? Collections.emptyMap()
                        : topicTagAnalyticsRepository
                        .findByLearnerAnalyticsSnapshot_IdAndTopicTagIn(snapshotId, topicTagList)
                        .stream()
                        .collect(Collectors.toMap(
                                LearnerTopicTagAnalytics::getTopicTag,
                                Function.identity()
                        ));

        // 5. Build AI input
        List<AIInput> inputs = buildAIInputs(
                weaknesses,
                strengths,
                questionAnalyticsMap,
                topicAnalyticsMap
        );

        List<AIInput> weaknessInputs = inputs.stream()
                .filter(i -> i.getType().equals("WEAKNESS"))
                .toList();

        List<AIInput> strengthInputs = inputs.stream()
                .filter(i -> i.getType().equals("STRENGTH"))
                .toList();

// tasks phải build riêng (quan trọng)
        List<TaskInput>  taskInputs = buildTaskInputs(studyPlanId);


        // 6. Call AI (batch)
        StudyPlanAIResponse res = aiClient.generateWithRetry(
                weaknessInputs,
                strengthInputs,
                taskInputs
        );



        updater.saveAIResult(studyPlanId, res);
    }



    private void extractFocusFromWeakness(
            List<LearnerStudyPlanWeaknessBlock> blocks,
            Set<PracticeQuestionType> questionTypes,
            Set<PracticeTopicTag> topicTags
    ) {
        for (LearnerStudyPlanWeaknessBlock block : blocks) {
            if (block.getFocusType() == LearnerStudyPlanFocusType.QUESTION_TYPE) {
                questionTypes.add(block.getQuestionType());
            } else {
                topicTags.add(block.getTopicTag());
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
                questionTypes.add(block.getQuestionType());
            } else {
                topicTags.add(block.getTopicTag());
            }
        }
    }

    private List<AIInput> buildAIInputs(
            List<LearnerStudyPlanWeaknessBlock> weaknesses,
            List<LearnerStudyPlanStrengthBlock> strengths,
            Map<PracticeQuestionType, LearnerQuestionTypeAnalytics> qMap,
            Map<PracticeTopicTag, LearnerTopicTagAnalytics> tMap
    ) {
        List<AIInput> inputs = new ArrayList<>();

        for (LearnerStudyPlanWeaknessBlock w : weaknesses) {
            inputs.add(buildWeaknessInput(w, qMap, tMap));
        }

        for (LearnerStudyPlanStrengthBlock s : strengths) {
            inputs.add(buildStrengthInput(s, qMap, tMap));
        }

        return inputs;
    }

    private List<TaskInput> buildTaskInputs(String studyPlanId) {
        List<LearnerStudyPlanTask> tasks =
                taskRepository.findTasksByStudyPlanId(studyPlanId);

        List<TaskInput> inputs = new ArrayList<>();

        for (var t : tasks) {
            inputs.add(new TaskInput(
                    t.getId(),
                    t.getFocusType(),
                    t.getQuestionType(),
                    t.getTopicTag(),
                    t.getDirection()
            ));
        }

        return inputs;
    }

    private AIInput buildWeaknessInput(
            LearnerStudyPlanWeaknessBlock block,
            Map<PracticeQuestionType, LearnerQuestionTypeAnalytics> qMap,
            Map<PracticeTopicTag, LearnerTopicTagAnalytics> tMap
    ) {
        return buildInputCommon(
                block.getId(),
                "WEAKNESS",
                block.getFocusType(),
                block.getQuestionType(),
                block.getTopicTag(),
                qMap,
                tMap
        );
    }

    private AIInput buildStrengthInput(
            LearnerStudyPlanStrengthBlock block,
            Map<PracticeQuestionType, LearnerQuestionTypeAnalytics> qMap,
            Map<PracticeTopicTag, LearnerTopicTagAnalytics> tMap
    ) {
        return buildInputCommon(
                block.getId(),
                "STRENGTH",
                block.getFocusType(),
                block.getQuestionType(),
                block.getTopicTag(),
                qMap,
                tMap
        );
    }

    private AIInput buildInputCommon(
            String id,
            String type,
            LearnerStudyPlanFocusType focusType,
            PracticeQuestionType questionType,
            PracticeTopicTag topicTag,
            Map<PracticeQuestionType, LearnerQuestionTypeAnalytics> qMap,
            Map<PracticeTopicTag, LearnerTopicTagAnalytics> tMap
    ) {
        double correctRate = 0;
        double effectiveAccuracy = 0;
        double skipRate = 0;

        if (focusType == LearnerStudyPlanFocusType.QUESTION_TYPE) {
            var analytics = qMap.get(questionType);
            if (analytics != null) {
                correctRate = analytics.getRollingCorrectAnswerPercentage();
                effectiveAccuracy = analytics.getRollingEffectiveAccuracy();
                skipRate = analytics.getRollingSkipRate();
            }
        } else {
            var analytics = tMap.get(topicTag);
            if (analytics != null) {
                correctRate = analytics.getRollingCorrectAnswerPercentage();
                effectiveAccuracy = analytics.getRollingEffectiveAccuracy();
                skipRate = analytics.getRollingSkipRate();
            }
        }

        return new AIInput(
                id,
                type,
                focusType,
                questionType,
                topicTag,
                correctRate,
                effectiveAccuracy,
                skipRate
        );
    }
}