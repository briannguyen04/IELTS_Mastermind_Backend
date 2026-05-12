package com.ieltsmastermind.ai.feedback.business.service.LearnerStudyPlanAIService;

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
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanTaskDirection;
import com.ieltsmastermind.practice.studyplan.management.persistence.LearnerStudyPlanRepository;
import com.ieltsmastermind.practice.studyplan.management.persistence.LearnerStudyPlanStrengthBlockRepository;
import com.ieltsmastermind.practice.studyplan.management.persistence.LearnerStudyPlanTaskRepository;
import com.ieltsmastermind.practice.studyplan.management.persistence.LearnerStudyPlanWeaknessBlockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearnerStudyPlanAIServiceImplTest {

    @Mock
    private OpenAIClient aiClient;

    @Mock
    private LearnerStudyPlanRepository studyPlanRepository;

    @Mock
    private LearnerStudyPlanTaskRepository taskRepository;

    @Mock
    private LearnerStudyPlanStrengthBlockRepository strengthRepository;

    @Mock
    private LearnerStudyPlanWeaknessBlockRepository weaknessRepository;

    @Mock
    private LearnerStudyPlanAIUpdater updater;

    @InjectMocks
    private LearnerStudyPlanAIServiceImpl learnerStudyPlanAIService;

    @Test
    void triggerAIContentGeneration_whenListeningStudyPlanExists_shouldBuildInputsCallAIAndSaveResult() {
        String studyPlanId = "study-plan-1";
        LearnerStudyPlan studyPlan = studyPlan(studyPlanId, PracticeContentSkill.LISTENING);
        SubmissionAnalytics analytics = analytics(PracticeContentSkill.LISTENING);
        analytics.setFocusTypeAnalytics(new LinkedHashSet<>(List.of(
                questionAnalytics(PracticeQuestionType.MULTIPLE_CHOICE, 75.5, 0.0),
                questionAnalytics(PracticeQuestionType.MATCHING, 45.0, 0.0),
                topicAnalytics(PracticeTopicTag.EDUCATION_AND_LEARNING, 82.0, 0.0),
                topicAnalytics(PracticeTopicTag.TECHNOLOGY_INTERNET_AND_AI, 40.0, 0.0)
        )));
        studyPlan.setSubmissionAnalytics(analytics);

        LearnerStudyPlanWeaknessBlock weaknessQuestion =
                weaknessBlock("weakness-question", LearnerStudyPlanFocusType.QUESTION_TYPE,
                        PracticeQuestionType.MATCHING, null);
        LearnerStudyPlanWeaknessBlock weaknessTopic =
                weaknessBlock("weakness-topic", LearnerStudyPlanFocusType.TOPIC,
                        null, PracticeTopicTag.TECHNOLOGY_INTERNET_AND_AI);
        LearnerStudyPlanStrengthBlock strengthQuestion =
                strengthBlock("strength-question", LearnerStudyPlanFocusType.QUESTION_TYPE,
                        PracticeQuestionType.MULTIPLE_CHOICE, null);
        LearnerStudyPlanStrengthBlock strengthTopic =
                strengthBlock("strength-topic", LearnerStudyPlanFocusType.TOPIC,
                        null, PracticeTopicTag.EDUCATION_AND_LEARNING);

        LearnerStudyPlanTask taskQuestion =
                task("task-question", LearnerStudyPlanFocusType.QUESTION_TYPE,
                        PracticeQuestionType.MATCHING, null, LearnerStudyPlanTaskDirection.INCREASE);
        LearnerStudyPlanTask taskTopic =
                task("task-topic", LearnerStudyPlanFocusType.TOPIC,
                        null, PracticeTopicTag.TECHNOLOGY_INTERNET_AND_AI, LearnerStudyPlanTaskDirection.REDUCE);

        StudyPlanAIResponse aiResponse = new StudyPlanAIResponse();

        when(studyPlanRepository.findById(studyPlanId)).thenReturn(Optional.of(studyPlan));
        when(weaknessRepository.findWeaknessBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of(weaknessQuestion, weaknessTopic));
        when(strengthRepository.findStrengthBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of(strengthQuestion, strengthTopic));
        when(taskRepository.findTasksByStudyPlanId(studyPlanId))
                .thenReturn(List.of(taskQuestion, taskTopic));
        when(aiClient.generateWithRetry(anyList(), anyList(), anyList(), anyBoolean()))
                .thenReturn(aiResponse);

        learnerStudyPlanAIService.triggerAIContentGeneration(studyPlanId);

        ArgumentCaptor<List<AIInput>> weaknessInputsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<AIInput>> strengthInputsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<TaskInput>> taskInputsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Boolean> isWritingCaptor = ArgumentCaptor.forClass(Boolean.class);

        verify(aiClient).generateWithRetry(
                weaknessInputsCaptor.capture(),
                strengthInputsCaptor.capture(),
                taskInputsCaptor.capture(),
                isWritingCaptor.capture()
        );

        assertThat(isWritingCaptor.getValue()).isFalse();

        List<AIInput> weaknessInputs = weaknessInputsCaptor.getValue();
        List<AIInput> strengthInputs = strengthInputsCaptor.getValue();
        List<TaskInput> taskInputs = taskInputsCaptor.getValue();

        assertThat(weaknessInputs).hasSize(2);
        assertThat(weaknessInputs.get(0).getId()).isEqualTo("weakness-question");
        assertThat(weaknessInputs.get(0).getType()).isEqualTo("WEAKNESS");
        assertThat(weaknessInputs.get(0).getFocusType()).isEqualTo(LearnerStudyPlanFocusType.QUESTION_TYPE);
        assertThat(weaknessInputs.get(0).getQuestionType()).isEqualTo(PracticeQuestionType.MATCHING);
        assertThat(weaknessInputs.get(0).getTopicTag()).isNull();
        assertThat(weaknessInputs.get(0).getCorrectRate()).isEqualTo(45.0);

        assertThat(weaknessInputs.get(1).getId()).isEqualTo("weakness-topic");
        assertThat(weaknessInputs.get(1).getType()).isEqualTo("WEAKNESS");
        assertThat(weaknessInputs.get(1).getFocusType()).isEqualTo(LearnerStudyPlanFocusType.TOPIC);
        assertThat(weaknessInputs.get(1).getQuestionType()).isNull();
        assertThat(weaknessInputs.get(1).getTopicTag()).isEqualTo(PracticeTopicTag.TECHNOLOGY_INTERNET_AND_AI);
        assertThat(weaknessInputs.get(1).getCorrectRate()).isEqualTo(40.0);

        assertThat(strengthInputs).hasSize(2);
        assertThat(strengthInputs.get(0).getId()).isEqualTo("strength-question");
        assertThat(strengthInputs.get(0).getType()).isEqualTo("STRENGTH");
        assertThat(strengthInputs.get(0).getQuestionType()).isEqualTo(PracticeQuestionType.MULTIPLE_CHOICE);
        assertThat(strengthInputs.get(0).getCorrectRate()).isEqualTo(75.5);

        assertThat(strengthInputs.get(1).getId()).isEqualTo("strength-topic");
        assertThat(strengthInputs.get(1).getType()).isEqualTo("STRENGTH");
        assertThat(strengthInputs.get(1).getTopicTag()).isEqualTo(PracticeTopicTag.EDUCATION_AND_LEARNING);
        assertThat(strengthInputs.get(1).getCorrectRate()).isEqualTo(82.0);

        assertThat(taskInputs).hasSize(2);
        assertThat(taskInputs.get(0).getId()).isEqualTo("task-question");
        assertThat(taskInputs.get(0).getFocusType()).isEqualTo(LearnerStudyPlanFocusType.QUESTION_TYPE);
        assertThat(taskInputs.get(0).getQuestionType()).isEqualTo(PracticeQuestionType.MATCHING);
        assertThat(taskInputs.get(0).getTopicTag()).isNull();
        assertThat(taskInputs.get(0).getDirection()).isEqualTo(LearnerStudyPlanTaskDirection.INCREASE);

        assertThat(taskInputs.get(1).getId()).isEqualTo("task-topic");
        assertThat(taskInputs.get(1).getFocusType()).isEqualTo(LearnerStudyPlanFocusType.TOPIC);
        assertThat(taskInputs.get(1).getQuestionType()).isNull();
        assertThat(taskInputs.get(1).getTopicTag()).isEqualTo(PracticeTopicTag.TECHNOLOGY_INTERNET_AND_AI);
        assertThat(taskInputs.get(1).getDirection()).isEqualTo(LearnerStudyPlanTaskDirection.REDUCE);

        verify(updater).saveAIResult(studyPlanId, aiResponse);
        verify(studyPlanRepository).findById(studyPlanId);
        verify(weaknessRepository).findWeaknessBlocksByStudyPlanId(studyPlanId);
        verify(strengthRepository).findStrengthBlocksByStudyPlanId(studyPlanId);
        verify(taskRepository).findTasksByStudyPlanId(studyPlanId);
    }

    @Test
    void triggerAIContentGeneration_whenWritingStudyPlanExists_shouldUseBandScoresAsCorrectRateAndSetWritingFlag() {
        String studyPlanId = "study-plan-1";
        LearnerStudyPlan studyPlan = studyPlan(studyPlanId, PracticeContentSkill.WRITING);
        SubmissionAnalytics analytics = analytics(PracticeContentSkill.WRITING);
        analytics.setFocusTypeAnalytics(new LinkedHashSet<>(List.of(
                questionAnalytics(PracticeQuestionType.OPINION, 0.0, 7.5),
                topicAnalytics(PracticeTopicTag.WORK_JOBS_AND_CAREERS, 0.0, 8.0)
        )));
        studyPlan.setSubmissionAnalytics(analytics);

        LearnerStudyPlanWeaknessBlock weakness =
                weaknessBlock("weakness-question", LearnerStudyPlanFocusType.QUESTION_TYPE,
                        PracticeQuestionType.OPINION, null);
        LearnerStudyPlanStrengthBlock strength =
                strengthBlock("strength-topic", LearnerStudyPlanFocusType.TOPIC,
                        null, PracticeTopicTag.WORK_JOBS_AND_CAREERS);

        StudyPlanAIResponse aiResponse = new StudyPlanAIResponse();

        when(studyPlanRepository.findById(studyPlanId)).thenReturn(Optional.of(studyPlan));
        when(weaknessRepository.findWeaknessBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of(weakness));
        when(strengthRepository.findStrengthBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of(strength));
        when(taskRepository.findTasksByStudyPlanId(studyPlanId)).thenReturn(List.of());
        when(aiClient.generateWithRetry(anyList(), anyList(), anyList(), anyBoolean()))
                .thenReturn(aiResponse);

        learnerStudyPlanAIService.triggerAIContentGeneration(studyPlanId);

        ArgumentCaptor<List<AIInput>> weaknessInputsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<AIInput>> strengthInputsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<TaskInput>> taskInputsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Boolean> isWritingCaptor = ArgumentCaptor.forClass(Boolean.class);

        verify(aiClient).generateWithRetry(
                weaknessInputsCaptor.capture(),
                strengthInputsCaptor.capture(),
                taskInputsCaptor.capture(),
                isWritingCaptor.capture()
        );

        assertThat(isWritingCaptor.getValue()).isTrue();
        assertThat(weaknessInputsCaptor.getValue()).hasSize(1);
        assertThat(weaknessInputsCaptor.getValue().get(0).getCorrectRate()).isEqualTo(7.5);
        assertThat(strengthInputsCaptor.getValue()).hasSize(1);
        assertThat(strengthInputsCaptor.getValue().get(0).getCorrectRate()).isEqualTo(8.0);
        assertThat(taskInputsCaptor.getValue()).isEmpty();

        verify(updater).saveAIResult(studyPlanId, aiResponse);
    }

    @Test
    void triggerAIContentGeneration_whenAnalyticsForBlockIsMissingOrNull_shouldUseZeroMetricValue() {
        String studyPlanId = "study-plan-1";
        LearnerStudyPlan studyPlan = studyPlan(studyPlanId, PracticeContentSkill.LISTENING);
        SubmissionAnalytics analytics = analytics(PracticeContentSkill.LISTENING);
        analytics.setFocusTypeAnalytics(new LinkedHashSet<>(List.of(
                questionAnalytics(PracticeQuestionType.MULTIPLE_CHOICE, null, 0.0)
        )));
        studyPlan.setSubmissionAnalytics(analytics);

        LearnerStudyPlanWeaknessBlock weaknessWithNullMetric =
                weaknessBlock("weakness-null-metric", LearnerStudyPlanFocusType.QUESTION_TYPE,
                        PracticeQuestionType.MULTIPLE_CHOICE, null);
        LearnerStudyPlanWeaknessBlock weaknessWithoutAnalytics =
                weaknessBlock("weakness-missing-analytics", LearnerStudyPlanFocusType.TOPIC,
                        null, PracticeTopicTag.TECHNOLOGY_INTERNET_AND_AI);

        when(studyPlanRepository.findById(studyPlanId)).thenReturn(Optional.of(studyPlan));
        when(weaknessRepository.findWeaknessBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of(weaknessWithNullMetric, weaknessWithoutAnalytics));
        when(strengthRepository.findStrengthBlocksByStudyPlanId(studyPlanId)).thenReturn(List.of());
        when(taskRepository.findTasksByStudyPlanId(studyPlanId)).thenReturn(List.of());
        when(aiClient.generateWithRetry(anyList(), anyList(), anyList(), anyBoolean()))
                .thenReturn(new StudyPlanAIResponse());

        learnerStudyPlanAIService.triggerAIContentGeneration(studyPlanId);

        ArgumentCaptor<List<AIInput>> weaknessInputsCaptor = ArgumentCaptor.forClass(List.class);
        verify(aiClient).generateWithRetry(
                weaknessInputsCaptor.capture(),
                anyList(),
                anyList(),
                anyBoolean()
        );

        assertThat(weaknessInputsCaptor.getValue()).hasSize(2);
        assertThat(weaknessInputsCaptor.getValue())
                .extracting(AIInput::getCorrectRate)
                .containsExactly(0.0, 0.0);
    }

    @Test
    void triggerAIContentGeneration_whenBlocksHaveNullFocusValues_shouldIgnoreNullFocusForAnalyticsLookupAndStillBuildInputs() {
        String studyPlanId = "study-plan-1";
        LearnerStudyPlan studyPlan = studyPlan(studyPlanId, PracticeContentSkill.LISTENING);
        SubmissionAnalytics analytics = analytics(PracticeContentSkill.LISTENING);
        analytics.setFocusTypeAnalytics(new LinkedHashSet<>(List.of(
                questionAnalytics(PracticeQuestionType.MULTIPLE_CHOICE, 90.0, 0.0),
                topicAnalytics(PracticeTopicTag.EDUCATION_AND_LEARNING, 80.0, 0.0)
        )));
        studyPlan.setSubmissionAnalytics(analytics);

        LearnerStudyPlanWeaknessBlock weaknessQuestionWithNullQuestionType =
                weaknessBlock("weakness-question-null", LearnerStudyPlanFocusType.QUESTION_TYPE, null, null);
        LearnerStudyPlanStrengthBlock strengthTopicWithNullTopicTag =
                strengthBlock("strength-topic-null", LearnerStudyPlanFocusType.TOPIC, null, null);

        when(studyPlanRepository.findById(studyPlanId)).thenReturn(Optional.of(studyPlan));
        when(weaknessRepository.findWeaknessBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of(weaknessQuestionWithNullQuestionType));
        when(strengthRepository.findStrengthBlocksByStudyPlanId(studyPlanId))
                .thenReturn(List.of(strengthTopicWithNullTopicTag));
        when(taskRepository.findTasksByStudyPlanId(studyPlanId)).thenReturn(List.of());
        when(aiClient.generateWithRetry(anyList(), anyList(), anyList(), anyBoolean()))
                .thenReturn(new StudyPlanAIResponse());

        learnerStudyPlanAIService.triggerAIContentGeneration(studyPlanId);

        ArgumentCaptor<List<AIInput>> weaknessInputsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<AIInput>> strengthInputsCaptor = ArgumentCaptor.forClass(List.class);

        verify(aiClient).generateWithRetry(
                weaknessInputsCaptor.capture(),
                strengthInputsCaptor.capture(),
                anyList(),
                anyBoolean()
        );

        assertThat(weaknessInputsCaptor.getValue()).hasSize(1);
        assertThat(weaknessInputsCaptor.getValue().get(0).getId()).isEqualTo("weakness-question-null");
        assertThat(weaknessInputsCaptor.getValue().get(0).getQuestionType()).isNull();
        assertThat(weaknessInputsCaptor.getValue().get(0).getCorrectRate()).isEqualTo(0.0);

        assertThat(strengthInputsCaptor.getValue()).hasSize(1);
        assertThat(strengthInputsCaptor.getValue().get(0).getId()).isEqualTo("strength-topic-null");
        assertThat(strengthInputsCaptor.getValue().get(0).getTopicTag()).isNull();
        assertThat(strengthInputsCaptor.getValue().get(0).getCorrectRate()).isEqualTo(0.0);
    }

    @Test
    void triggerAIContentGeneration_whenStudyPlanDoesNotExist_shouldThrowRuntimeExceptionAndSkipLoadingBlocks() {
        when(studyPlanRepository.findById("missing-plan")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> learnerStudyPlanAIService.triggerAIContentGeneration("missing-plan")
        );

        assertThat(exception.getMessage()).isEqualTo("Study plan not found");

        verify(studyPlanRepository).findById("missing-plan");
        verify(weaknessRepository, never()).findWeaknessBlocksByStudyPlanId("missing-plan");
        verify(strengthRepository, never()).findStrengthBlocksByStudyPlanId("missing-plan");
        verify(taskRepository, never()).findTasksByStudyPlanId("missing-plan");
        verify(aiClient, never()).generateWithRetry(anyList(), anyList(), anyList(), anyBoolean());
        verify(updater, never()).saveAIResult(anyString(), any(StudyPlanAIResponse.class));
    }

    @Test
    void triggerAIContentGeneration_whenSubmissionAnalyticsIsNull_shouldThrowRuntimeExceptionAndSkipLoadingBlocks() {
        LearnerStudyPlan studyPlan = studyPlan("study-plan-1", PracticeContentSkill.LISTENING);
        studyPlan.setSubmissionAnalytics(null);

        when(studyPlanRepository.findById("study-plan-1")).thenReturn(Optional.of(studyPlan));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> learnerStudyPlanAIService.triggerAIContentGeneration("study-plan-1")
        );

        assertThat(exception.getMessage()).isEqualTo("Submission analytics not found");

        verify(weaknessRepository, never()).findWeaknessBlocksByStudyPlanId("study-plan-1");
        verify(strengthRepository, never()).findStrengthBlocksByStudyPlanId("study-plan-1");
        verify(taskRepository, never()).findTasksByStudyPlanId("study-plan-1");
        verify(aiClient, never()).generateWithRetry(anyList(), anyList(), anyList(), anyBoolean());
        verify(updater, never()).saveAIResult(anyString(), any(StudyPlanAIResponse.class));
    }

    @Test
    void triggerAIContentGeneration_whenAIClientThrows_shouldPropagateExceptionAndSkipUpdater() {
        String studyPlanId = "study-plan-1";
        LearnerStudyPlan studyPlan = studyPlan(studyPlanId, PracticeContentSkill.LISTENING);
        studyPlan.setSubmissionAnalytics(analytics(PracticeContentSkill.LISTENING));

        when(studyPlanRepository.findById(studyPlanId)).thenReturn(Optional.of(studyPlan));
        when(weaknessRepository.findWeaknessBlocksByStudyPlanId(studyPlanId)).thenReturn(List.of());
        when(strengthRepository.findStrengthBlocksByStudyPlanId(studyPlanId)).thenReturn(List.of());
        when(taskRepository.findTasksByStudyPlanId(studyPlanId)).thenReturn(List.of());
        when(aiClient.generateWithRetry(anyList(), anyList(), anyList(), anyBoolean()))
                .thenThrow(new RuntimeException("AI failed"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> learnerStudyPlanAIService.triggerAIContentGeneration(studyPlanId)
        );

        assertThat(exception.getMessage()).isEqualTo("AI failed");

        verify(aiClient).generateWithRetry(anyList(), anyList(), anyList(), anyBoolean());
        verify(updater, never()).saveAIResult(anyString(), any(StudyPlanAIResponse.class));
    }

    @Test
    void triggerAIContentGeneration_whenNoBlocksAndNoTasksExist_shouldCallAIWithEmptyListsAndSaveResult() {
        String studyPlanId = "study-plan-1";
        LearnerStudyPlan studyPlan = studyPlan(studyPlanId, PracticeContentSkill.LISTENING);
        studyPlan.setSubmissionAnalytics(analytics(PracticeContentSkill.LISTENING));
        StudyPlanAIResponse response = new StudyPlanAIResponse();

        when(studyPlanRepository.findById(studyPlanId)).thenReturn(Optional.of(studyPlan));
        when(weaknessRepository.findWeaknessBlocksByStudyPlanId(studyPlanId)).thenReturn(List.of());
        when(strengthRepository.findStrengthBlocksByStudyPlanId(studyPlanId)).thenReturn(List.of());
        when(taskRepository.findTasksByStudyPlanId(studyPlanId)).thenReturn(List.of());
        when(aiClient.generateWithRetry(anyList(), anyList(), anyList(), anyBoolean())).thenReturn(response);

        learnerStudyPlanAIService.triggerAIContentGeneration(studyPlanId);

        ArgumentCaptor<List<AIInput>> weaknessInputsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<AIInput>> strengthInputsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<TaskInput>> taskInputsCaptor = ArgumentCaptor.forClass(List.class);

        verify(aiClient).generateWithRetry(
                weaknessInputsCaptor.capture(),
                strengthInputsCaptor.capture(),
                taskInputsCaptor.capture(),
                anyBoolean()
        );

        assertThat(weaknessInputsCaptor.getValue()).isEmpty();
        assertThat(strengthInputsCaptor.getValue()).isEmpty();
        assertThat(taskInputsCaptor.getValue()).isEmpty();

        verify(updater).saveAIResult(studyPlanId, response);
    }

    @Test
    void privateBuildQuestionTypeAnalyticsMap_whenQuestionTypesAreEmpty_shouldReturnEmptyMap() {
        SubmissionAnalytics analytics = analytics(PracticeContentSkill.LISTENING);
        analytics.setFocusTypeAnalytics(new LinkedHashSet<>(List.of(
                questionAnalytics(PracticeQuestionType.MULTIPLE_CHOICE, 75.0, 0.0)
        )));

        Map<PracticeQuestionType, FocusTypeAnalytics> result = invokePrivateMethod(
                "buildQuestionTypeAnalyticsMap",
                new Class<?>[]{SubmissionAnalytics.class, Set.class},
                analytics,
                Set.of()
        );

        assertThat(result).isEmpty();
    }

    @Test
    void privateBuildQuestionTypeAnalyticsMap_whenDuplicatesExist_shouldKeepFirstMatchingAnalytics() {
        SubmissionAnalytics analytics = analytics(PracticeContentSkill.LISTENING);
        FocusTypeAnalytics first = questionAnalytics(PracticeQuestionType.MULTIPLE_CHOICE, 75.0, 0.0);
        FocusTypeAnalytics duplicate = questionAnalytics(PracticeQuestionType.MULTIPLE_CHOICE, 90.0, 0.0);
        FocusTypeAnalytics ignoredTopic = topicAnalytics(PracticeTopicTag.EDUCATION_AND_LEARNING, 80.0, 0.0);
        FocusTypeAnalytics nullQuestionType = questionAnalytics(null, 55.0, 0.0);

        analytics.setFocusTypeAnalytics(new LinkedHashSet<>(List.of(
                first,
                duplicate,
                ignoredTopic,
                nullQuestionType
        )));

        Map<PracticeQuestionType, FocusTypeAnalytics> result = invokePrivateMethod(
                "buildQuestionTypeAnalyticsMap",
                new Class<?>[]{SubmissionAnalytics.class, Set.class},
                analytics,
                Set.of(PracticeQuestionType.MULTIPLE_CHOICE)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(PracticeQuestionType.MULTIPLE_CHOICE)).isSameAs(first);
    }

    @Test
    void privateBuildTopicTagAnalyticsMap_whenTopicTagsAreEmpty_shouldReturnEmptyMap() {
        SubmissionAnalytics analytics = analytics(PracticeContentSkill.LISTENING);
        analytics.setFocusTypeAnalytics(new LinkedHashSet<>(List.of(
                topicAnalytics(PracticeTopicTag.EDUCATION_AND_LEARNING, 80.0, 0.0)
        )));

        Map<PracticeTopicTag, FocusTypeAnalytics> result = invokePrivateMethod(
                "buildTopicTagAnalyticsMap",
                new Class<?>[]{SubmissionAnalytics.class, Set.class},
                analytics,
                Set.of()
        );

        assertThat(result).isEmpty();
    }

    @Test
    void privateBuildTopicTagAnalyticsMap_whenDuplicatesExist_shouldKeepFirstMatchingAnalytics() {
        SubmissionAnalytics analytics = analytics(PracticeContentSkill.LISTENING);
        FocusTypeAnalytics first = topicAnalytics(PracticeTopicTag.EDUCATION_AND_LEARNING, 80.0, 0.0);
        FocusTypeAnalytics duplicate = topicAnalytics(PracticeTopicTag.EDUCATION_AND_LEARNING, 90.0, 0.0);
        FocusTypeAnalytics ignoredQuestionType = questionAnalytics(PracticeQuestionType.MULTIPLE_CHOICE, 75.0, 0.0);
        FocusTypeAnalytics nullTopicTag = topicAnalytics(null, 55.0, 0.0);

        analytics.setFocusTypeAnalytics(new LinkedHashSet<>(List.of(
                first,
                duplicate,
                ignoredQuestionType,
                nullTopicTag
        )));

        Map<PracticeTopicTag, FocusTypeAnalytics> result = invokePrivateMethod(
                "buildTopicTagAnalyticsMap",
                new Class<?>[]{SubmissionAnalytics.class, Set.class},
                analytics,
                Set.of(PracticeTopicTag.EDUCATION_AND_LEARNING)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(PracticeTopicTag.EDUCATION_AND_LEARNING)).isSameAs(first);
    }

    @Test
    void privateSafeDouble_whenValueIsNullOrProvided_shouldReturnExpectedValue() {
        Double nullResult = invokePrivateMethod(
                "safeDouble",
                new Class<?>[]{Double.class},
                new Object[]{null}
        );
        Double valueResult = invokePrivateMethod(
                "safeDouble",
                new Class<?>[]{Double.class},
                7.5
        );

        assertThat(nullResult).isEqualTo(0.0);
        assertThat(valueResult).isEqualTo(7.5);
    }

    private LearnerStudyPlan studyPlan(String id, PracticeContentSkill skill) {
        LearnerStudyPlan studyPlan = new LearnerStudyPlan();

        studyPlan.setId(id);
        studyPlan.setSkill(skill);

        return studyPlan;
    }

    private SubmissionAnalytics analytics(PracticeContentSkill skill) {
        SubmissionAnalytics analytics = new SubmissionAnalytics();

        analytics.setId("analytics-1");
        analytics.setSkill(skill);
        analytics.setFocusTypeAnalytics(new LinkedHashSet<>());

        return analytics;
    }

    private FocusTypeAnalytics questionAnalytics(PracticeQuestionType questionType,
                                                 Double rollingCorrectAnswerPercentage,
                                                 Double rollingOverallBandScore) {
        FocusTypeAnalytics analytics = new FocusTypeAnalytics();

        analytics.setId("analytics-question-" + (questionType == null ? "null" : questionType.name()));
        analytics.setFocusType(LearnerStudyPlanFocusType.QUESTION_TYPE);
        analytics.setQuestionType(questionType);
        analytics.setTopicTag(null);
        analytics.setRollingCorrectAnswerPercentage(rollingCorrectAnswerPercentage);
        analytics.setRollingOverallBandScore(rollingOverallBandScore);

        return analytics;
    }

    private FocusTypeAnalytics topicAnalytics(PracticeTopicTag topicTag,
                                              Double rollingCorrectAnswerPercentage,
                                              Double rollingOverallBandScore) {
        FocusTypeAnalytics analytics = new FocusTypeAnalytics();

        analytics.setId("analytics-topic-" + (topicTag == null ? "null" : topicTag.name()));
        analytics.setFocusType(LearnerStudyPlanFocusType.TOPIC);
        analytics.setQuestionType(null);
        analytics.setTopicTag(topicTag);
        analytics.setRollingCorrectAnswerPercentage(rollingCorrectAnswerPercentage);
        analytics.setRollingOverallBandScore(rollingOverallBandScore);

        return analytics;
    }

    private LearnerStudyPlanWeaknessBlock weaknessBlock(String id,
                                                        LearnerStudyPlanFocusType focusType,
                                                        PracticeQuestionType questionType,
                                                        PracticeTopicTag topicTag) {
        LearnerStudyPlanWeaknessBlock block = new LearnerStudyPlanWeaknessBlock();

        block.setId(id);
        block.setFocusType(focusType);
        block.setQuestionType(questionType);
        block.setTopicTag(topicTag);

        return block;
    }

    private LearnerStudyPlanStrengthBlock strengthBlock(String id,
                                                        LearnerStudyPlanFocusType focusType,
                                                        PracticeQuestionType questionType,
                                                        PracticeTopicTag topicTag) {
        LearnerStudyPlanStrengthBlock block = new LearnerStudyPlanStrengthBlock();

        block.setId(id);
        block.setFocusType(focusType);
        block.setQuestionType(questionType);
        block.setTopicTag(topicTag);

        return block;
    }

    private LearnerStudyPlanTask task(String id,
                                      LearnerStudyPlanFocusType focusType,
                                      PracticeQuestionType questionType,
                                      PracticeTopicTag topicTag,
                                      LearnerStudyPlanTaskDirection direction) {
        LearnerStudyPlanTask task = new LearnerStudyPlanTask();

        task.setId(id);
        task.setFocusType(focusType);
        task.setQuestionType(questionType);
        task.setTopicTag(topicTag);
        task.setDirection(direction);

        return task;
    }

    @SuppressWarnings("unchecked")
    private <T> T invokePrivateMethod(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = LearnerStudyPlanAIServiceImpl.class.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return (T) method.invoke(learnerStudyPlanAIService, args);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();

            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            throw new RuntimeException("Failed to invoke " + methodName, cause);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to invoke " + methodName, exception);
        }
    }
}
