package com.ieltsmastermind.practice.studyplan.management.business.service;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.analytics.management.business.interfaces.SubmissionAnalyticsService;
import com.ieltsmastermind.practice.analytics.management.domain.entity.FocusTypeAnalytics;
import com.ieltsmastermind.practice.analytics.management.domain.entity.SubmissionAnalytics;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTopicTag;
import com.ieltsmastermind.practice.studyplan.management.domain.dto.LearnerStudyPlanActiveCheckResponseDto;
import com.ieltsmastermind.practice.studyplan.management.domain.dto.LearnerStudyPlanCreateRequestDto;
import com.ieltsmastermind.practice.studyplan.management.domain.dto.LearnerStudyPlanResponseDto;
import com.ieltsmastermind.practice.studyplan.management.domain.entity.LearnerStudyPlan;
import com.ieltsmastermind.practice.studyplan.management.domain.entity.LearnerStudyPlanStrengthBlock;
import com.ieltsmastermind.practice.studyplan.management.domain.entity.LearnerStudyPlanTask;
import com.ieltsmastermind.practice.studyplan.management.domain.entity.LearnerStudyPlanWeaknessBlock;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanFocusType;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanStatus;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanTargetMetric;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanTaskDirection;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanTaskStatus;
import com.ieltsmastermind.practice.studyplan.management.persistence.LearnerStudyPlanRepository;
import com.ieltsmastermind.user.management.domain.entity.User;
import com.ieltsmastermind.user.management.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearnerStudyPlanServiceImplTest {

    @Mock
    private LearnerStudyPlanRepository learnerStudyPlanRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubmissionAnalyticsService submissionAnalyticsService;

    @Mock
    private IncludeSpec includes;

    @InjectMocks
    private LearnerStudyPlanServiceImpl learnerStudyPlanService;

    @Test
    void create_whenListeningAnalyticsHasEnoughSubmissionsAndLatestPlanExists_shouldDeactivateLatestGeneratePlanAndReturnPlanId() {
        User learner = learner("learner-1");
        LearnerStudyPlanCreateRequestDto request = createRequest(PracticeContentSkill.LISTENING);
        SubmissionAnalytics analytics = analyticsSnapshot(
                "analytics-1",
                learner,
                PracticeContentSkill.LISTENING,
                5
        );
        analytics.setFocusTypeAnalytics(new LinkedHashSet<>(List.of(
                questionTypeAnalytics(analytics, PracticeQuestionType.MULTIPLE_CHOICE, 90.0, 0.0, 10),
                questionTypeAnalytics(analytics, PracticeQuestionType.MATCHING, 40.0, 0.0, 10),
                topicAnalytics(analytics, PracticeTopicTag.EDUCATION_AND_LEARNING, 75.0, 0.0, 10),
                topicAnalytics(analytics, PracticeTopicTag.TECHNOLOGY_INTERNET_AND_AI, 55.0, 0.0, 10),
                topicAnalytics(analytics, PracticeTopicTag.WORK_JOBS_AND_CAREERS, 60.0, 0.0, 10)
        )));
        LearnerStudyPlan latestStudyPlan = studyPlan("latest-plan", learner, PracticeContentSkill.LISTENING);
        latestStudyPlan.setVersionNumber(2);
        latestStudyPlan.setStatus(LearnerStudyPlanStatus.ACTIVE);

        when(userRepository.findById("learner-1")).thenReturn(Optional.of(learner));
        when(submissionAnalyticsService.createSnapshot("learner-1", PracticeContentSkill.LISTENING))
                .thenReturn(analytics);
        when(learnerStudyPlanRepository.findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(
                "learner-1",
                PracticeContentSkill.LISTENING
        )).thenReturn(Optional.of(latestStudyPlan));
        when(learnerStudyPlanRepository.save(any(LearnerStudyPlan.class))).thenAnswer(invocation -> {
            LearnerStudyPlan studyPlan = invocation.getArgument(0);
            studyPlan.setId("study-plan-1");
            return studyPlan;
        });

        LearnerStudyPlanResponseDto result = learnerStudyPlanService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("study-plan-1");
        assertThat(latestStudyPlan.getStatus()).isEqualTo(LearnerStudyPlanStatus.INACTIVE);

        ArgumentCaptor<LearnerStudyPlan> studyPlanCaptor =
                ArgumentCaptor.forClass(LearnerStudyPlan.class);
        verify(learnerStudyPlanRepository).save(studyPlanCaptor.capture());

        LearnerStudyPlan savedStudyPlan = studyPlanCaptor.getValue();

        assertThat(savedStudyPlan.getUser()).isSameAs(learner);
        assertThat(savedStudyPlan.getSkill()).isEqualTo(PracticeContentSkill.LISTENING);
        assertThat(savedStudyPlan.getVersionNumber()).isEqualTo(3);
        assertThat(savedStudyPlan.getStatus()).isEqualTo(LearnerStudyPlanStatus.ACTIVE);
        assertThat(savedStudyPlan.getSubmissionCountUsed()).isEqualTo(5);
        assertThat(savedStudyPlan.getSubmissionCountSinceCreation()).isZero();
        assertThat(savedStudyPlan.getSubmissionAnalytics()).isSameAs(analytics);
        assertThat(savedStudyPlan.isReadyToFinalize()).isFalse();

        assertThat(savedStudyPlan.getStrengthBlocks()).hasSize(4);
        assertThat(savedStudyPlan.getStrengthBlocks().get(0).getFocusType())
                .isEqualTo(LearnerStudyPlanFocusType.QUESTION_TYPE);
        assertThat(savedStudyPlan.getStrengthBlocks().get(0).getQuestionType())
                .isEqualTo(PracticeQuestionType.MULTIPLE_CHOICE);
        assertThat(savedStudyPlan.getStrengthBlocks().get(0).getStrengthRank()).isEqualTo(1);
        assertThat(savedStudyPlan.getStrengthBlocks().get(0).getLearnerStudyPlan()).isSameAs(savedStudyPlan);

        assertThat(savedStudyPlan.getWeaknessBlocks()).hasSize(4);
        assertThat(savedStudyPlan.getWeaknessBlocks().get(0).getFocusType())
                .isEqualTo(LearnerStudyPlanFocusType.QUESTION_TYPE);
        assertThat(savedStudyPlan.getWeaknessBlocks().get(0).getQuestionType())
                .isEqualTo(PracticeQuestionType.MATCHING);
        assertThat(savedStudyPlan.getWeaknessBlocks().get(0).getWeaknessRank()).isEqualTo(1);
        assertThat(savedStudyPlan.getWeaknessBlocks().get(0).getLearnerStudyPlan()).isSameAs(savedStudyPlan);

        assertThat(savedStudyPlan.getTasks()).hasSize(4);
        LearnerStudyPlanTask firstTask = savedStudyPlan.getTasks().get(0);
        assertThat(firstTask.getLearnerStudyPlan()).isSameAs(savedStudyPlan);
        assertThat(firstTask.getPriorityRank()).isEqualTo(1);
        assertThat(firstTask.getFocusType()).isEqualTo(LearnerStudyPlanFocusType.QUESTION_TYPE);
        assertThat(firstTask.getQuestionType()).isEqualTo(PracticeQuestionType.MATCHING);
        assertThat(firstTask.getTopicTag()).isNull();
        assertThat(firstTask.getTargetMetric())
                .isEqualTo(LearnerStudyPlanTargetMetric.ROLLING_CORRECT_ANSWER_PERCENTAGE);
        assertThat(firstTask.getCurrentValue()).isEqualTo(40.0);
        assertThat(firstTask.getTargetValue()).isEqualTo(45.0);
        assertThat(firstTask.getDirection()).isEqualTo(LearnerStudyPlanTaskDirection.INCREASE);
        assertThat(firstTask.getStatus()).isEqualTo(LearnerStudyPlanTaskStatus.ACTIVE);

        verify(userRepository).findById("learner-1");
        verify(submissionAnalyticsService).createSnapshot("learner-1", PracticeContentSkill.LISTENING);
    }

    @Test
    void create_whenWritingAnalyticsHasEnoughSubmissions_shouldGenerateWritingBandScoreTasksAndCapTargetAtNine() {
        User learner = learner("learner-1");
        LearnerStudyPlanCreateRequestDto request = createRequest(PracticeContentSkill.WRITING);
        SubmissionAnalytics analytics = analyticsSnapshot(
                "analytics-1",
                learner,
                PracticeContentSkill.WRITING,
                5
        );
        analytics.setFocusTypeAnalytics(new LinkedHashSet<>(List.of(
                questionTypeAnalytics(analytics, PracticeQuestionType.OPINION, 0.0, 8.8, 2),
                questionTypeAnalytics(analytics, PracticeQuestionType.PROBLEM_SOLUTION, 0.0, 5.0, 1),
                topicAnalytics(analytics, PracticeTopicTag.WORK_JOBS_AND_CAREERS, 0.0, 6.0, 2),
                topicAnalytics(analytics, PracticeTopicTag.EDUCATION_AND_LEARNING, 0.0, 7.5, 2)
        )));

        when(userRepository.findById("learner-1")).thenReturn(Optional.of(learner));
        when(submissionAnalyticsService.createSnapshot("learner-1", PracticeContentSkill.WRITING))
                .thenReturn(analytics);
        when(learnerStudyPlanRepository.findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(
                "learner-1",
                PracticeContentSkill.WRITING
        )).thenReturn(Optional.empty());
        when(learnerStudyPlanRepository.save(any(LearnerStudyPlan.class))).thenAnswer(invocation -> {
            LearnerStudyPlan studyPlan = invocation.getArgument(0);
            studyPlan.setId("study-plan-1");
            return studyPlan;
        });

        LearnerStudyPlanResponseDto result = learnerStudyPlanService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("study-plan-1");

        ArgumentCaptor<LearnerStudyPlan> studyPlanCaptor =
                ArgumentCaptor.forClass(LearnerStudyPlan.class);
        verify(learnerStudyPlanRepository).save(studyPlanCaptor.capture());

        LearnerStudyPlan savedStudyPlan = studyPlanCaptor.getValue();

        assertThat(savedStudyPlan.getVersionNumber()).isEqualTo(1);
        assertThat(savedStudyPlan.getTasks()).hasSize(4);

        LearnerStudyPlanTask firstTask = savedStudyPlan.getTasks().get(0);
        assertThat(firstTask.getQuestionType()).isEqualTo(PracticeQuestionType.PROBLEM_SOLUTION);
        assertThat(firstTask.getTargetMetric())
                .isEqualTo(LearnerStudyPlanTargetMetric.ROLLING_OVERALL_BAND_SCORE);
        assertThat(firstTask.getCurrentValue()).isEqualTo(5.0);
        assertThat(firstTask.getTargetValue()).isEqualTo(5.5);

        LearnerStudyPlanTask cappedTask = savedStudyPlan.getTasks().stream()
                .filter(task -> task.getQuestionType() == PracticeQuestionType.OPINION)
                .findFirst()
                .orElseThrow();
        assertThat(cappedTask.getCurrentValue()).isEqualTo(8.8);
        assertThat(cappedTask.getTargetValue()).isEqualTo(9.0);

        assertThat(savedStudyPlan.getStrengthBlocks().get(0).getQuestionType())
                .isEqualTo(PracticeQuestionType.OPINION);
        assertThat(savedStudyPlan.getWeaknessBlocks().get(0).getQuestionType())
                .isEqualTo(PracticeQuestionType.PROBLEM_SOLUTION);
    }

    @Test
    void create_whenLearnerDoesNotExist_shouldThrowIllegalArgumentExceptionAndSkipSnapshot() {
        LearnerStudyPlanCreateRequestDto request = createRequest(PracticeContentSkill.LISTENING);

        when(userRepository.findById("learner-1")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> learnerStudyPlanService.create(request)
        );

        assertThat(exception.getMessage()).isEqualTo("Learner not found");

        verify(userRepository).findById("learner-1");
        verify(submissionAnalyticsService, never()).createSnapshot(anyString(), any(PracticeContentSkill.class));
        verify(learnerStudyPlanRepository, never()).save(any(LearnerStudyPlan.class));
    }

    @Test
    void create_whenAnalyticsHasLessThanFiveSubmissions_shouldThrowIllegalArgumentExceptionAndSkipSave() {
        User learner = learner("learner-1");
        LearnerStudyPlanCreateRequestDto request = createRequest(PracticeContentSkill.LISTENING);
        SubmissionAnalytics analytics = analyticsSnapshot(
                "analytics-1",
                learner,
                PracticeContentSkill.LISTENING,
                4
        );

        when(userRepository.findById("learner-1")).thenReturn(Optional.of(learner));
        when(submissionAnalyticsService.createSnapshot("learner-1", PracticeContentSkill.LISTENING))
                .thenReturn(analytics);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> learnerStudyPlanService.create(request)
        );

        assertThat(exception.getMessage())
                .isEqualTo("At least 5 submissions are required for learner and skill");

        verify(learnerStudyPlanRepository, never())
                .findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(anyString(), any(PracticeContentSkill.class));
        verify(learnerStudyPlanRepository, never()).save(any(LearnerStudyPlan.class));
    }

    @Test
    void getActiveStudyPlanByUserIdAndSkill_whenPlanExistsAndFieldsIncluded_shouldReturnDtoWithIncludedFields() {
        LearnerStudyPlan studyPlan = fullStudyPlan();

        when(learnerStudyPlanRepository.findTopByUser_UserIdAndSkillAndStatusOrderByVersionNumberDesc(
                "learner-1",
                PracticeContentSkill.LISTENING,
                LearnerStudyPlanStatus.ACTIVE
        )).thenReturn(Optional.of(studyPlan));
        mockIncludes(
                "skill",
                "versionnumber",
                "status",
                "submissioncountused",
                "submissioncountsincecreation",
                "readytofinalize",
                "user.userid",
                "user.email",
                "user.firstname",
                "user.lastname",
                "tasks.id",
                "tasks.focustype",
                "tasks.questiontype",
                "tasks.topictag",
                "tasks.targetmetric",
                "tasks.currentvalue",
                "tasks.targetvalue",
                "tasks.direction",
                "tasks.status",
                "tasks.description",
                "strengthblocks.id",
                "strengthblocks.focustype",
                "strengthblocks.questiontype",
                "strengthblocks.topictag",
                "strengthblocks.description",
                "strengthblocks.explanation",
                "strengthblocks.evidence",
                "strengthblocks.recommendednextaction",
                "weaknessblocks.id",
                "weaknessblocks.focustype",
                "weaknessblocks.questiontype",
                "weaknessblocks.topictag",
                "weaknessblocks.description",
                "weaknessblocks.explanation",
                "weaknessblocks.evidence",
                "weaknessblocks.recommendednextaction"
        );

        LearnerStudyPlanResponseDto result =
                learnerStudyPlanService.getActiveStudyPlanByUserIdAndSkill(
                        "learner-1",
                        PracticeContentSkill.LISTENING,
                        includes
                );

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("study-plan-1");
        assertThat(result.getSkill()).isEqualTo(PracticeContentSkill.LISTENING);
        assertThat(result.getVersionNumber()).isEqualTo(2);
        assertThat(result.getStatus()).isEqualTo(LearnerStudyPlanStatus.ACTIVE);
        assertThat(result.getSubmissionCountUsed()).isEqualTo(5);
        assertThat(result.getSubmissionCountSinceCreation()).isEqualTo(3);
        assertThat(result.getReadyToFinalize()).isTrue();

        assertThat(result.getUser()).isNotNull();
        assertThat(result.getUser().getUserId()).isEqualTo("learner-1");
        assertThat(result.getUser().getEmail()).isEqualTo("learner@example.com");
        assertThat(result.getUser().getFirstname()).isEqualTo("Learner");
        assertThat(result.getUser().getLastname()).isEqualTo("User");

        assertThat(result.getTasks()).hasSize(1);
        assertThat(result.getTasks().get(0).getId()).isEqualTo("task-1");
        assertThat(result.getTasks().get(0).getFocusType()).isEqualTo(LearnerStudyPlanFocusType.QUESTION_TYPE);
        assertThat(result.getTasks().get(0).getQuestionType()).isEqualTo(PracticeQuestionType.MULTIPLE_CHOICE);
        assertThat(result.getTasks().get(0).getTopicTag()).isNull();
        assertThat(result.getTasks().get(0).getTargetMetric())
                .isEqualTo(LearnerStudyPlanTargetMetric.ROLLING_CORRECT_ANSWER_PERCENTAGE);
        assertThat(result.getTasks().get(0).getCurrentValue()).isEqualTo(80.0);
        assertThat(result.getTasks().get(0).getTargetValue()).isEqualTo(85.0);
        assertThat(result.getTasks().get(0).getDirection()).isEqualTo(LearnerStudyPlanTaskDirection.INCREASE);
        assertThat(result.getTasks().get(0).getStatus()).isEqualTo(LearnerStudyPlanTaskStatus.ACTIVE);
        assertThat(result.getTasks().get(0).getDescription()).isEqualTo("Improve multiple choice accuracy.");

        assertThat(result.getStrengthBlocks()).hasSize(1);
        assertThat(result.getStrengthBlocks().get(0).getId()).isEqualTo("strength-1");
        assertThat(result.getStrengthBlocks().get(0).getFocusType()).isEqualTo(LearnerStudyPlanFocusType.TOPIC);
        assertThat(result.getStrengthBlocks().get(0).getTopicTag())
                .isEqualTo(PracticeTopicTag.EDUCATION_AND_LEARNING);
        assertThat(result.getStrengthBlocks().get(0).getDescription()).isEqualTo("Strong topic performance.");
        assertThat(result.getStrengthBlocks().get(0).getExplanation()).isEqualTo("Consistent high accuracy.");
        assertThat(result.getStrengthBlocks().get(0).getEvidence()).isEqualTo("90% rolling accuracy.");
        assertThat(result.getStrengthBlocks().get(0).getRecommendedNextAction()).isEqualTo("Maintain practice.");

        assertThat(result.getWeaknessBlocks()).hasSize(1);
        assertThat(result.getWeaknessBlocks().get(0).getId()).isEqualTo("weakness-1");
        assertThat(result.getWeaknessBlocks().get(0).getFocusType()).isEqualTo(LearnerStudyPlanFocusType.QUESTION_TYPE);
        assertThat(result.getWeaknessBlocks().get(0).getQuestionType()).isEqualTo(PracticeQuestionType.MATCHING);
        assertThat(result.getWeaknessBlocks().get(0).getDescription()).isEqualTo("Weak question type performance.");
        assertThat(result.getWeaknessBlocks().get(0).getExplanation()).isEqualTo("Accuracy is below target.");
        assertThat(result.getWeaknessBlocks().get(0).getEvidence()).isEqualTo("45% rolling accuracy.");
        assertThat(result.getWeaknessBlocks().get(0).getRecommendedNextAction()).isEqualTo("Review matching strategies.");

        verify(learnerStudyPlanRepository).findTopByUser_UserIdAndSkillAndStatusOrderByVersionNumberDesc(
                "learner-1",
                PracticeContentSkill.LISTENING,
                LearnerStudyPlanStatus.ACTIVE
        );
    }

    @Test
    void getActiveStudyPlanByUserIdAndSkill_whenNoFieldsIncluded_shouldReturnOnlyPlanId() {
        LearnerStudyPlan studyPlan = fullStudyPlan();

        when(learnerStudyPlanRepository.findTopByUser_UserIdAndSkillAndStatusOrderByVersionNumberDesc(
                "learner-1",
                PracticeContentSkill.LISTENING,
                LearnerStudyPlanStatus.ACTIVE
        )).thenReturn(Optional.of(studyPlan));
        mockIncludes();

        LearnerStudyPlanResponseDto result =
                learnerStudyPlanService.getActiveStudyPlanByUserIdAndSkill(
                        "learner-1",
                        PracticeContentSkill.LISTENING,
                        includes
                );

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("study-plan-1");
        assertThat(result.getSkill()).isNull();
        assertThat(result.getVersionNumber()).isNull();
        assertThat(result.getStatus()).isNull();
        assertThat(result.getUser()).isNull();
        assertThat(result.getTasks()).isNull();
        assertThat(result.getStrengthBlocks()).isNull();
        assertThat(result.getWeaknessBlocks()).isNull();
    }

    @Test
    void getActiveStudyPlanByUserIdAndSkill_whenUserIncludeRequestedButPlanUserIsNull_shouldNotSetUserDto() {
        LearnerStudyPlan studyPlan = fullStudyPlan();
        studyPlan.setUser(null);

        when(learnerStudyPlanRepository.findTopByUser_UserIdAndSkillAndStatusOrderByVersionNumberDesc(
                "learner-1",
                PracticeContentSkill.LISTENING,
                LearnerStudyPlanStatus.ACTIVE
        )).thenReturn(Optional.of(studyPlan));
        mockIncludes("user.userid", "user.email");

        LearnerStudyPlanResponseDto result =
                learnerStudyPlanService.getActiveStudyPlanByUserIdAndSkill(
                        "learner-1",
                        PracticeContentSkill.LISTENING,
                        includes
                );

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("study-plan-1");
        assertThat(result.getUser()).isNull();
    }

    @Test
    void getActiveStudyPlanByUserIdAndSkill_whenPlanDoesNotExist_shouldThrowIllegalArgumentException() {
        when(learnerStudyPlanRepository.findTopByUser_UserIdAndSkillAndStatusOrderByVersionNumberDesc(
                "learner-1",
                PracticeContentSkill.LISTENING,
                LearnerStudyPlanStatus.ACTIVE
        )).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> learnerStudyPlanService.getActiveStudyPlanByUserIdAndSkill(
                        "learner-1",
                        PracticeContentSkill.LISTENING,
                        includes
                )
        );

        assertThat(exception.getMessage()).isEqualTo("Active learner study plan not found");
    }

    @Test
    void getHasActiveStudyPlan_whenLearnerExistsAndActivePlanExists_shouldReturnTrue() {
        when(userRepository.findById("learner-1")).thenReturn(Optional.of(learner("learner-1")));
        when(learnerStudyPlanRepository.existsByUser_UserIdAndSkillAndStatus(
                "learner-1",
                PracticeContentSkill.LISTENING,
                LearnerStudyPlanStatus.ACTIVE
        )).thenReturn(true);

        LearnerStudyPlanActiveCheckResponseDto result =
                learnerStudyPlanService.getHasActiveStudyPlan("learner-1", PracticeContentSkill.LISTENING);

        assertThat(result).isNotNull();
        assertThat(result.getHasActiveStudyPlan()).isTrue();

        verify(userRepository).findById("learner-1");
        verify(learnerStudyPlanRepository).existsByUser_UserIdAndSkillAndStatus(
                "learner-1",
                PracticeContentSkill.LISTENING,
                LearnerStudyPlanStatus.ACTIVE
        );
    }

    @Test
    void getHasActiveStudyPlan_whenLearnerExistsAndActivePlanDoesNotExist_shouldReturnFalse() {
        when(userRepository.findById("learner-1")).thenReturn(Optional.of(learner("learner-1")));
        when(learnerStudyPlanRepository.existsByUser_UserIdAndSkillAndStatus(
                "learner-1",
                PracticeContentSkill.LISTENING,
                LearnerStudyPlanStatus.ACTIVE
        )).thenReturn(false);

        LearnerStudyPlanActiveCheckResponseDto result =
                learnerStudyPlanService.getHasActiveStudyPlan("learner-1", PracticeContentSkill.LISTENING);

        assertThat(result).isNotNull();
        assertThat(result.getHasActiveStudyPlan()).isFalse();
    }

    @Test
    void getHasActiveStudyPlan_whenLearnerDoesNotExist_shouldThrowIllegalArgumentExceptionAndSkipExistsLookup() {
        when(userRepository.findById("missing-learner")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> learnerStudyPlanService.getHasActiveStudyPlan(
                        "missing-learner",
                        PracticeContentSkill.LISTENING
                )
        );

        assertThat(exception.getMessage()).isEqualTo("Learner not found");

        verify(learnerStudyPlanRepository, never())
                .existsByUser_UserIdAndSkillAndStatus(anyString(), any(PracticeContentSkill.class), any(LearnerStudyPlanStatus.class));
    }

    @Test
    void refreshStudyPlan_whenPlanExistsAndThresholdsAreMet_shouldRefreshTasksSetReadyToFinalizeSaveAndReturnPlanId() {
        String userId = "learner-1";
        PracticeContentSkill skill = PracticeContentSkill.LISTENING;
        LearnerStudyPlan studyPlan = studyPlan("study-plan-1", learner(userId), skill);
        studyPlan.setSubmissionCountUsed(5);
        studyPlan.getTasks().add(task(
                "task-1",
                studyPlan,
                1,
                LearnerStudyPlanFocusType.QUESTION_TYPE,
                PracticeQuestionType.MULTIPLE_CHOICE,
                null,
                LearnerStudyPlanTargetMetric.ROLLING_CORRECT_ANSWER_PERCENTAGE,
                60.0,
                80.0,
                LearnerStudyPlanTaskDirection.INCREASE,
                LearnerStudyPlanTaskStatus.ACTIVE
        ));
        studyPlan.getTasks().add(task(
                "task-2",
                studyPlan,
                2,
                LearnerStudyPlanFocusType.TOPIC,
                null,
                PracticeTopicTag.EDUCATION_AND_LEARNING,
                LearnerStudyPlanTargetMetric.ROLLING_CORRECT_ANSWER_PERCENTAGE,
                40.0,
                50.0,
                LearnerStudyPlanTaskDirection.INCREASE,
                LearnerStudyPlanTaskStatus.ACTIVE
        ));
        studyPlan.getTasks().add(task(
                "task-3",
                studyPlan,
                3,
                LearnerStudyPlanFocusType.TOPIC,
                null,
                PracticeTopicTag.TECHNOLOGY_INTERNET_AND_AI,
                LearnerStudyPlanTargetMetric.ROLLING_CORRECT_ANSWER_PERCENTAGE,
                90.0,
                70.0,
                LearnerStudyPlanTaskDirection.REDUCE,
                LearnerStudyPlanTaskStatus.ACTIVE
        ));
        studyPlan.getTasks().add(task(
                "task-4",
                studyPlan,
                4,
                LearnerStudyPlanFocusType.QUESTION_TYPE,
                PracticeQuestionType.MATCHING,
                null,
                LearnerStudyPlanTargetMetric.ROLLING_CORRECT_ANSWER_PERCENTAGE,
                80.0,
                30.0,
                LearnerStudyPlanTaskDirection.REDUCE,
                LearnerStudyPlanTaskStatus.ACTIVE
        ));

        SubmissionAnalytics analytics = analyticsSnapshot("analytics-1", learner(userId), skill, 10);

        when(userRepository.findById(userId)).thenReturn(Optional.of(learner(userId)));
        when(learnerStudyPlanRepository.findTopByUser_UserIdAndSkillAndStatusOrderByVersionNumberDesc(
                userId,
                skill,
                LearnerStudyPlanStatus.ACTIVE
        )).thenReturn(Optional.of(studyPlan));
        when(submissionAnalyticsService.createSnapshot(userId, skill)).thenReturn(analytics);
        when(submissionAnalyticsService.getCurrentValue(
                LearnerStudyPlanFocusType.QUESTION_TYPE,
                LearnerStudyPlanTargetMetric.ROLLING_CORRECT_ANSWER_PERCENTAGE,
                userId,
                skill,
                PracticeQuestionType.MULTIPLE_CHOICE,
                null
        )).thenReturn(85.0);
        when(submissionAnalyticsService.getCurrentValue(
                LearnerStudyPlanFocusType.TOPIC,
                LearnerStudyPlanTargetMetric.ROLLING_CORRECT_ANSWER_PERCENTAGE,
                userId,
                skill,
                null,
                PracticeTopicTag.EDUCATION_AND_LEARNING
        )).thenReturn(55.0);
        when(submissionAnalyticsService.getCurrentValue(
                LearnerStudyPlanFocusType.TOPIC,
                LearnerStudyPlanTargetMetric.ROLLING_CORRECT_ANSWER_PERCENTAGE,
                userId,
                skill,
                null,
                PracticeTopicTag.TECHNOLOGY_INTERNET_AND_AI
        )).thenReturn(60.0);
        when(submissionAnalyticsService.getCurrentValue(
                LearnerStudyPlanFocusType.QUESTION_TYPE,
                LearnerStudyPlanTargetMetric.ROLLING_CORRECT_ANSWER_PERCENTAGE,
                userId,
                skill,
                PracticeQuestionType.MATCHING,
                null
        )).thenReturn(35.0);
        when(learnerStudyPlanRepository.save(any(LearnerStudyPlan.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LearnerStudyPlanResponseDto result =
                learnerStudyPlanService.refreshStudyPlan(userId, skill);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("study-plan-1");

        assertThat(studyPlan.getSubmissionCountSinceCreation()).isEqualTo(5);
        assertThat(studyPlan.getTasks().get(0).getCurrentValue()).isEqualTo(85.0);
        assertThat(studyPlan.getTasks().get(0).getStatus()).isEqualTo(LearnerStudyPlanTaskStatus.COMPLETED);
        assertThat(studyPlan.getTasks().get(1).getCurrentValue()).isEqualTo(55.0);
        assertThat(studyPlan.getTasks().get(1).getStatus()).isEqualTo(LearnerStudyPlanTaskStatus.COMPLETED);
        assertThat(studyPlan.getTasks().get(2).getCurrentValue()).isEqualTo(60.0);
        assertThat(studyPlan.getTasks().get(2).getStatus()).isEqualTo(LearnerStudyPlanTaskStatus.COMPLETED);
        assertThat(studyPlan.getTasks().get(3).getCurrentValue()).isEqualTo(35.0);
        assertThat(studyPlan.getTasks().get(3).getStatus()).isEqualTo(LearnerStudyPlanTaskStatus.ACTIVE);
        assertThat(studyPlan.isReadyToFinalize()).isTrue();

        verify(learnerStudyPlanRepository).save(studyPlan);
    }

    @Test
    void refreshStudyPlan_whenCurrentValueIsNullAndSubmissionDeltaIsNegative_shouldUseZeroCurrentValueAndKeepReadyFalse() {
        String userId = "learner-1";
        PracticeContentSkill skill = PracticeContentSkill.LISTENING;
        LearnerStudyPlan studyPlan = studyPlan("study-plan-1", learner(userId), skill);
        studyPlan.setSubmissionCountUsed(10);
        studyPlan.getTasks().add(task(
                "task-1",
                studyPlan,
                1,
                LearnerStudyPlanFocusType.QUESTION_TYPE,
                PracticeQuestionType.MULTIPLE_CHOICE,
                null,
                LearnerStudyPlanTargetMetric.ROLLING_CORRECT_ANSWER_PERCENTAGE,
                60.0,
                80.0,
                LearnerStudyPlanTaskDirection.INCREASE,
                LearnerStudyPlanTaskStatus.ACTIVE
        ));
        SubmissionAnalytics analytics = analyticsSnapshot("analytics-1", learner(userId), skill, 7);

        when(userRepository.findById(userId)).thenReturn(Optional.of(learner(userId)));
        when(learnerStudyPlanRepository.findTopByUser_UserIdAndSkillAndStatusOrderByVersionNumberDesc(
                userId,
                skill,
                LearnerStudyPlanStatus.ACTIVE
        )).thenReturn(Optional.of(studyPlan));
        when(submissionAnalyticsService.createSnapshot(userId, skill)).thenReturn(analytics);
        when(submissionAnalyticsService.getCurrentValue(
                LearnerStudyPlanFocusType.QUESTION_TYPE,
                LearnerStudyPlanTargetMetric.ROLLING_CORRECT_ANSWER_PERCENTAGE,
                userId,
                skill,
                PracticeQuestionType.MULTIPLE_CHOICE,
                null
        )).thenReturn(null);
        when(learnerStudyPlanRepository.save(any(LearnerStudyPlan.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LearnerStudyPlanResponseDto result =
                learnerStudyPlanService.refreshStudyPlan(userId, skill);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("study-plan-1");
        assertThat(studyPlan.getSubmissionCountSinceCreation()).isZero();
        assertThat(studyPlan.getTasks().get(0).getCurrentValue()).isEqualTo(0.0);
        assertThat(studyPlan.getTasks().get(0).getStatus()).isEqualTo(LearnerStudyPlanTaskStatus.ACTIVE);
        assertThat(studyPlan.isReadyToFinalize()).isFalse();
    }

    @Test
    void refreshStudyPlan_whenTasksAreEmpty_shouldKeepReadyToFinalizeFalse() {
        String userId = "learner-1";
        PracticeContentSkill skill = PracticeContentSkill.LISTENING;
        LearnerStudyPlan studyPlan = studyPlan("study-plan-1", learner(userId), skill);
        studyPlan.setSubmissionCountUsed(5);
        SubmissionAnalytics analytics = analyticsSnapshot("analytics-1", learner(userId), skill, 20);

        when(userRepository.findById(userId)).thenReturn(Optional.of(learner(userId)));
        when(learnerStudyPlanRepository.findTopByUser_UserIdAndSkillAndStatusOrderByVersionNumberDesc(
                userId,
                skill,
                LearnerStudyPlanStatus.ACTIVE
        )).thenReturn(Optional.of(studyPlan));
        when(submissionAnalyticsService.createSnapshot(userId, skill)).thenReturn(analytics);
        when(learnerStudyPlanRepository.save(any(LearnerStudyPlan.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LearnerStudyPlanResponseDto result =
                learnerStudyPlanService.refreshStudyPlan(userId, skill);

        assertThat(result).isNotNull();
        assertThat(studyPlan.getSubmissionCountSinceCreation()).isEqualTo(15);
        assertThat(studyPlan.isReadyToFinalize()).isFalse();

        verify(submissionAnalyticsService, never()).getCurrentValue(
                any(LearnerStudyPlanFocusType.class),
                any(LearnerStudyPlanTargetMetric.class),
                anyString(),
                any(PracticeContentSkill.class),
                any(PracticeQuestionType.class),
                any(PracticeTopicTag.class)
        );
    }

    @Test
    void refreshStudyPlan_whenLearnerDoesNotExist_shouldThrowIllegalArgumentExceptionAndSkipPlanLookup() {
        when(userRepository.findById("missing-learner")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> learnerStudyPlanService.refreshStudyPlan(
                        "missing-learner",
                        PracticeContentSkill.LISTENING
                )
        );

        assertThat(exception.getMessage()).isEqualTo("Learner not found");

        verify(learnerStudyPlanRepository, never())
                .findTopByUser_UserIdAndSkillAndStatusOrderByVersionNumberDesc(
                        anyString(),
                        any(PracticeContentSkill.class),
                        any(LearnerStudyPlanStatus.class)
                );
    }

    @Test
    void refreshStudyPlan_whenActivePlanDoesNotExist_shouldThrowIllegalArgumentExceptionAndSkipSnapshot() {
        when(userRepository.findById("learner-1")).thenReturn(Optional.of(learner("learner-1")));
        when(learnerStudyPlanRepository.findTopByUser_UserIdAndSkillAndStatusOrderByVersionNumberDesc(
                "learner-1",
                PracticeContentSkill.LISTENING,
                LearnerStudyPlanStatus.ACTIVE
        )).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> learnerStudyPlanService.refreshStudyPlan(
                        "learner-1",
                        PracticeContentSkill.LISTENING
                )
        );

        assertThat(exception.getMessage()).isEqualTo("Active learner study plan not found");

        verify(submissionAnalyticsService, never()).createSnapshot(anyString(), any(PracticeContentSkill.class));
        verify(learnerStudyPlanRepository, never()).save(any(LearnerStudyPlan.class));
    }

    @Test
    void finalizeStudyPlanById_whenPlanExists_shouldSetInactiveSaveAndReturnPlanId() {
        LearnerStudyPlan studyPlan = studyPlan("study-plan-1", learner("learner-1"), PracticeContentSkill.LISTENING);
        studyPlan.setStatus(LearnerStudyPlanStatus.ACTIVE);

        when(learnerStudyPlanRepository.findById("study-plan-1")).thenReturn(Optional.of(studyPlan));
        when(learnerStudyPlanRepository.save(any(LearnerStudyPlan.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LearnerStudyPlanResponseDto result =
                learnerStudyPlanService.finalizeStudyPlanById("study-plan-1");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("study-plan-1");
        assertThat(studyPlan.getStatus()).isEqualTo(LearnerStudyPlanStatus.INACTIVE);

        verify(learnerStudyPlanRepository).findById("study-plan-1");
        verify(learnerStudyPlanRepository).save(studyPlan);
    }

    @Test
    void finalizeStudyPlanById_whenPlanDoesNotExist_shouldThrowIllegalArgumentExceptionAndSkipSave() {
        when(learnerStudyPlanRepository.findById("missing-plan")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> learnerStudyPlanService.finalizeStudyPlanById("missing-plan")
        );

        assertThat(exception.getMessage()).isEqualTo("Learner study plan not found");

        verify(learnerStudyPlanRepository).findById("missing-plan");
        verify(learnerStudyPlanRepository, never()).save(any(LearnerStudyPlan.class));
    }

    @Test
    void privateHelpers_whenCalledWithBoundaryValues_shouldReturnExpectedValues() {
        Double writingTarget = invokePrivateMethod(
                "resolveTargetValue",
                new Class<?>[]{double.class, boolean.class},
                8.8,
                true
        );
        Double listeningTarget = invokePrivateMethod(
                "resolveTargetValue",
                new Class<?>[]{double.class, boolean.class},
                98.0,
                false
        );
        Double safeDouble = invokePrivateMethod(
                "safeDouble",
                new Class<?>[]{Double.class},
                new Object[]{null}
        );

        assertThat(writingTarget).isEqualTo(9.0);
        assertThat(listeningTarget).isEqualTo(100.0);
        assertThat(safeDouble).isEqualTo(0.0);
    }

    @Test
    void getActiveStudyPlanByUserIdAndSkill_whenOnlyTaskIdIncluded_shouldReturnTaskDtosWithOnlyIds() {
        LearnerStudyPlan studyPlan = fullStudyPlan();

        when(learnerStudyPlanRepository.findTopByUser_UserIdAndSkillAndStatusOrderByVersionNumberDesc(
                "learner-1",
                PracticeContentSkill.LISTENING,
                LearnerStudyPlanStatus.ACTIVE
        )).thenReturn(Optional.of(studyPlan));
        mockIncludes("tasks.id");

        LearnerStudyPlanResponseDto result =
                learnerStudyPlanService.getActiveStudyPlanByUserIdAndSkill(
                        "learner-1",
                        PracticeContentSkill.LISTENING,
                        includes
                );

        assertThat(result).isNotNull();
        assertThat(result.getTasks()).hasSize(1);
        assertThat(result.getTasks().get(0).getId()).isEqualTo("task-1");
        assertThat(result.getTasks().get(0).getFocusType()).isNull();
        assertThat(result.getTasks().get(0).getQuestionType()).isNull();
        assertThat(result.getTasks().get(0).getTopicTag()).isNull();
        assertThat(result.getTasks().get(0).getTargetMetric()).isNull();
        assertThat(result.getTasks().get(0).getCurrentValue()).isNull();
        assertThat(result.getTasks().get(0).getTargetValue()).isNull();
        assertThat(result.getTasks().get(0).getDirection()).isNull();
        assertThat(result.getTasks().get(0).getStatus()).isNull();
        assertThat(result.getTasks().get(0).getDescription()).isNull();
    }

    @Test
    void getActiveStudyPlanByUserIdAndSkill_whenOnlyUnusedStrengthStatusIncludeRequested_shouldCreateEmptyStrengthBlockDtos() {
        LearnerStudyPlan studyPlan = fullStudyPlan();

        when(learnerStudyPlanRepository.findTopByUser_UserIdAndSkillAndStatusOrderByVersionNumberDesc(
                "learner-1",
                PracticeContentSkill.LISTENING,
                LearnerStudyPlanStatus.ACTIVE
        )).thenReturn(Optional.of(studyPlan));
        mockIncludes("strengthblocks.status");

        LearnerStudyPlanResponseDto result =
                learnerStudyPlanService.getActiveStudyPlanByUserIdAndSkill(
                        "learner-1",
                        PracticeContentSkill.LISTENING,
                        includes
                );

        assertThat(result).isNotNull();
        assertThat(result.getStrengthBlocks()).hasSize(1);
        assertThat(result.getStrengthBlocks().get(0).getId()).isNull();
        assertThat(result.getStrengthBlocks().get(0).getFocusType()).isNull();
        assertThat(result.getStrengthBlocks().get(0).getQuestionType()).isNull();
        assertThat(result.getStrengthBlocks().get(0).getTopicTag()).isNull();
        assertThat(result.getStrengthBlocks().get(0).getDescription()).isNull();
        assertThat(result.getStrengthBlocks().get(0).getExplanation()).isNull();
        assertThat(result.getStrengthBlocks().get(0).getEvidence()).isNull();
        assertThat(result.getStrengthBlocks().get(0).getRecommendedNextAction()).isNull();
        assertThat(result.getWeaknessBlocks()).isNull();
    }

    @Test
    void getActiveStudyPlanByUserIdAndSkill_whenOnlyUnusedWeaknessStatusIncludeRequested_shouldCreateEmptyWeaknessBlockDtos() {
        LearnerStudyPlan studyPlan = fullStudyPlan();

        when(learnerStudyPlanRepository.findTopByUser_UserIdAndSkillAndStatusOrderByVersionNumberDesc(
                "learner-1",
                PracticeContentSkill.LISTENING,
                LearnerStudyPlanStatus.ACTIVE
        )).thenReturn(Optional.of(studyPlan));
        mockIncludes("weaknessblocks.analyticsconclusionlabel");

        LearnerStudyPlanResponseDto result =
                learnerStudyPlanService.getActiveStudyPlanByUserIdAndSkill(
                        "learner-1",
                        PracticeContentSkill.LISTENING,
                        includes
                );

        assertThat(result).isNotNull();
        assertThat(result.getWeaknessBlocks()).hasSize(1);
        assertThat(result.getWeaknessBlocks().get(0).getId()).isNull();
        assertThat(result.getWeaknessBlocks().get(0).getFocusType()).isNull();
        assertThat(result.getWeaknessBlocks().get(0).getQuestionType()).isNull();
        assertThat(result.getWeaknessBlocks().get(0).getTopicTag()).isNull();
        assertThat(result.getWeaknessBlocks().get(0).getDescription()).isNull();
        assertThat(result.getWeaknessBlocks().get(0).getExplanation()).isNull();
        assertThat(result.getWeaknessBlocks().get(0).getEvidence()).isNull();
        assertThat(result.getWeaknessBlocks().get(0).getRecommendedNextAction()).isNull();
        assertThat(result.getStrengthBlocks()).isNull();
    }

    @Test
    void refreshStudyPlan_whenTaskHasNullTargetValueAndReduceDirection_shouldTreatTargetAsZeroAndCompleteTask() {
        String userId = "learner-1";
        PracticeContentSkill skill = PracticeContentSkill.LISTENING;
        LearnerStudyPlan studyPlan = studyPlan("study-plan-1", learner(userId), skill);
        studyPlan.setSubmissionCountUsed(5);

        LearnerStudyPlanTask task = task(
                "task-1",
                studyPlan,
                1,
                LearnerStudyPlanFocusType.TOPIC,
                null,
                PracticeTopicTag.EDUCATION_AND_LEARNING,
                LearnerStudyPlanTargetMetric.ROLLING_CORRECT_ANSWER_PERCENTAGE,
                60.0,
                null,
                LearnerStudyPlanTaskDirection.REDUCE,
                LearnerStudyPlanTaskStatus.ACTIVE
        );
        studyPlan.getTasks().add(task);

        SubmissionAnalytics analytics = analyticsSnapshot("analytics-1", learner(userId), skill, 10);

        when(userRepository.findById(userId)).thenReturn(Optional.of(learner(userId)));
        when(learnerStudyPlanRepository.findTopByUser_UserIdAndSkillAndStatusOrderByVersionNumberDesc(
                userId,
                skill,
                LearnerStudyPlanStatus.ACTIVE
        )).thenReturn(Optional.of(studyPlan));
        when(submissionAnalyticsService.createSnapshot(userId, skill)).thenReturn(analytics);
        when(submissionAnalyticsService.getCurrentValue(
                LearnerStudyPlanFocusType.TOPIC,
                LearnerStudyPlanTargetMetric.ROLLING_CORRECT_ANSWER_PERCENTAGE,
                userId,
                skill,
                null,
                PracticeTopicTag.EDUCATION_AND_LEARNING
        )).thenReturn(0.0);
        when(learnerStudyPlanRepository.save(any(LearnerStudyPlan.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LearnerStudyPlanResponseDto result =
                learnerStudyPlanService.refreshStudyPlan(userId, skill);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("study-plan-1");
        assertThat(task.getCurrentValue()).isEqualTo(0.0);
        assertThat(task.getStatus()).isEqualTo(LearnerStudyPlanTaskStatus.COMPLETED);
        assertThat(studyPlan.getSubmissionCountSinceCreation()).isEqualTo(5);
        assertThat(studyPlan.isReadyToFinalize()).isTrue();
    }

    @Test
    void privateResolveStudyPlanReadyToFinalize_whenTasksAreNull_shouldReturnFalse() {
        LearnerStudyPlan studyPlan = studyPlan("study-plan-1", learner("learner-1"), PracticeContentSkill.LISTENING);
        studyPlan.setTasks(null);
        studyPlan.setSubmissionCountSinceCreation(10);

        Boolean result = invokePrivateMethod(
                "resolveStudyPlanReadyToFinalize",
                new Class<?>[]{LearnerStudyPlan.class},
                studyPlan
        );

        assertThat(result).isFalse();
    }

    @Test
    void privateResolveStudyPlanReadyToFinalize_whenSubmissionCountSinceCreationIsNull_shouldReturnFalse() {
        LearnerStudyPlan studyPlan = studyPlan("study-plan-1", learner("learner-1"), PracticeContentSkill.LISTENING);
        studyPlan.setSubmissionCountSinceCreation(null);
        studyPlan.getTasks().add(task(
                "task-1",
                studyPlan,
                1,
                LearnerStudyPlanFocusType.QUESTION_TYPE,
                PracticeQuestionType.MULTIPLE_CHOICE,
                null,
                LearnerStudyPlanTargetMetric.ROLLING_CORRECT_ANSWER_PERCENTAGE,
                80.0,
                80.0,
                LearnerStudyPlanTaskDirection.INCREASE,
                LearnerStudyPlanTaskStatus.COMPLETED
        ));

        Boolean result = invokePrivateMethod(
                "resolveStudyPlanReadyToFinalize",
                new Class<?>[]{LearnerStudyPlan.class},
                studyPlan
        );

        assertThat(result).isFalse();
    }

    @Test
    void privateResolveStudyPlanReadyToFinalize_whenCompletedTaskPercentageIsBelowThreshold_shouldReturnFalse() {
        LearnerStudyPlan studyPlan = studyPlan("study-plan-1", learner("learner-1"), PracticeContentSkill.LISTENING);
        studyPlan.setSubmissionCountSinceCreation(10);

        studyPlan.getTasks().add(task(
                "task-1",
                studyPlan,
                1,
                LearnerStudyPlanFocusType.QUESTION_TYPE,
                PracticeQuestionType.MULTIPLE_CHOICE,
                null,
                LearnerStudyPlanTargetMetric.ROLLING_CORRECT_ANSWER_PERCENTAGE,
                80.0,
                80.0,
                LearnerStudyPlanTaskDirection.INCREASE,
                LearnerStudyPlanTaskStatus.COMPLETED
        ));
        studyPlan.getTasks().add(task(
                "task-2",
                studyPlan,
                2,
                LearnerStudyPlanFocusType.TOPIC,
                null,
                PracticeTopicTag.EDUCATION_AND_LEARNING,
                LearnerStudyPlanTargetMetric.ROLLING_CORRECT_ANSWER_PERCENTAGE,
                50.0,
                80.0,
                LearnerStudyPlanTaskDirection.INCREASE,
                LearnerStudyPlanTaskStatus.ACTIVE
        ));

        Boolean result = invokePrivateMethod(
                "resolveStudyPlanReadyToFinalize",
                new Class<?>[]{LearnerStudyPlan.class},
                studyPlan
        );

        assertThat(result).isFalse();
    }

    @Test
    void privateResolveSubmissionCountSinceCreation_whenCountsAreNull_shouldReturnZero() {
        SubmissionAnalytics analytics = analyticsSnapshot(
                "analytics-1",
                learner("learner-1"),
                PracticeContentSkill.LISTENING,
                null
        );
        LearnerStudyPlan studyPlan = studyPlan("study-plan-1", learner("learner-1"), PracticeContentSkill.LISTENING);
        studyPlan.setSubmissionCountUsed(null);

        Integer result = invokePrivateMethod(
                "resolveSubmissionCountSinceCreation",
                new Class<?>[]{SubmissionAnalytics.class, LearnerStudyPlan.class},
                analytics,
                studyPlan
        );

        assertThat(result).isZero();
    }

    @Test
    void privateResolveTaskStatus_whenIncreaseCurrentValueIsBelowTarget_shouldReturnActive() {
        LearnerStudyPlan studyPlan = studyPlan("study-plan-1", learner("learner-1"), PracticeContentSkill.LISTENING);
        LearnerStudyPlanTask task = task(
                "task-1",
                studyPlan,
                1,
                LearnerStudyPlanFocusType.QUESTION_TYPE,
                PracticeQuestionType.MULTIPLE_CHOICE,
                null,
                LearnerStudyPlanTargetMetric.ROLLING_CORRECT_ANSWER_PERCENTAGE,
                70.0,
                80.0,
                LearnerStudyPlanTaskDirection.INCREASE,
                LearnerStudyPlanTaskStatus.ACTIVE
        );

        LearnerStudyPlanTaskStatus result = invokePrivateMethod(
                "resolveTaskStatus",
                new Class<?>[]{LearnerStudyPlanTask.class, double.class},
                task,
                70.0
        );

        assertThat(result).isEqualTo(LearnerStudyPlanTaskStatus.ACTIVE);
    }

    @Test
    void privateResolveTaskStatus_whenReduceCurrentValueEqualsTarget_shouldReturnCompleted() {
        LearnerStudyPlan studyPlan = studyPlan("study-plan-1", learner("learner-1"), PracticeContentSkill.LISTENING);
        LearnerStudyPlanTask task = task(
                "task-1",
                studyPlan,
                1,
                LearnerStudyPlanFocusType.TOPIC,
                null,
                PracticeTopicTag.EDUCATION_AND_LEARNING,
                LearnerStudyPlanTargetMetric.ROLLING_SKIP_RATE,
                20.0,
                20.0,
                LearnerStudyPlanTaskDirection.REDUCE,
                LearnerStudyPlanTaskStatus.ACTIVE
        );

        LearnerStudyPlanTaskStatus result = invokePrivateMethod(
                "resolveTaskStatus",
                new Class<?>[]{LearnerStudyPlanTask.class, double.class},
                task,
                20.0
        );

        assertThat(result).isEqualTo(LearnerStudyPlanTaskStatus.COMPLETED);
    }

    private LearnerStudyPlanCreateRequestDto createRequest(PracticeContentSkill skill) {
        LearnerStudyPlanCreateRequestDto request = new LearnerStudyPlanCreateRequestDto();

        request.setLearnerId("learner-1");
        request.setSkill(skill);

        return request;
    }

    private User learner(String userId) {
        User user = new User();

        user.setUserId(userId);
        user.setEmail("learner@example.com");
        user.setFirstname("Learner");
        user.setLastname("User");

        return user;
    }

    private SubmissionAnalytics analyticsSnapshot(
            String id,
            User learner,
            PracticeContentSkill skill,
            Integer submissionCountUsed
    ) {
        SubmissionAnalytics analytics = new SubmissionAnalytics();

        analytics.setId(id);
        analytics.setUser(learner);
        analytics.setSkill(skill);
        analytics.setVersionNumber(1);
        analytics.setSubmissionCountUsed(submissionCountUsed);
        analytics.setRollingCorrectAnswerPercentage(70.0);
        analytics.setRollingOverallBandScore(7.0);
        analytics.setFocusTypeAnalytics(new LinkedHashSet<>());

        return analytics;
    }

    private FocusTypeAnalytics questionTypeAnalytics(
            SubmissionAnalytics analytics,
            PracticeQuestionType questionType,
            Double rollingCorrectAnswerPercentage,
            Double rollingOverallBandScore,
            Integer exposureCount
    ) {
        FocusTypeAnalytics focusTypeAnalytics = new FocusTypeAnalytics();

        focusTypeAnalytics.setSubmissionAnalytics(analytics);
        focusTypeAnalytics.setFocusType(LearnerStudyPlanFocusType.QUESTION_TYPE);
        focusTypeAnalytics.setQuestionType(questionType);
        focusTypeAnalytics.setTopicTag(null);
        focusTypeAnalytics.setContributedSubmissionCount(1);
        focusTypeAnalytics.setRollingExposureCount(exposureCount);
        focusTypeAnalytics.setRollingCorrectAnswerPercentage(rollingCorrectAnswerPercentage);
        focusTypeAnalytics.setRollingOverallBandScore(rollingOverallBandScore);

        return focusTypeAnalytics;
    }

    private FocusTypeAnalytics topicAnalytics(
            SubmissionAnalytics analytics,
            PracticeTopicTag topicTag,
            Double rollingCorrectAnswerPercentage,
            Double rollingOverallBandScore,
            Integer exposureCount
    ) {
        FocusTypeAnalytics focusTypeAnalytics = new FocusTypeAnalytics();

        focusTypeAnalytics.setSubmissionAnalytics(analytics);
        focusTypeAnalytics.setFocusType(LearnerStudyPlanFocusType.TOPIC);
        focusTypeAnalytics.setQuestionType(null);
        focusTypeAnalytics.setTopicTag(topicTag);
        focusTypeAnalytics.setContributedSubmissionCount(1);
        focusTypeAnalytics.setRollingExposureCount(exposureCount);
        focusTypeAnalytics.setRollingCorrectAnswerPercentage(rollingCorrectAnswerPercentage);
        focusTypeAnalytics.setRollingOverallBandScore(rollingOverallBandScore);

        return focusTypeAnalytics;
    }

    private LearnerStudyPlan fullStudyPlan() {
        User learner = learner("learner-1");
        LearnerStudyPlan studyPlan = studyPlan("study-plan-1", learner, PracticeContentSkill.LISTENING);
        studyPlan.setVersionNumber(2);
        studyPlan.setStatus(LearnerStudyPlanStatus.ACTIVE);
        studyPlan.setSubmissionCountUsed(5);
        studyPlan.setSubmissionCountSinceCreation(3);
        studyPlan.setReadyToFinalize(true);

        LearnerStudyPlanTask task = task(
                "task-1",
                studyPlan,
                1,
                LearnerStudyPlanFocusType.QUESTION_TYPE,
                PracticeQuestionType.MULTIPLE_CHOICE,
                null,
                LearnerStudyPlanTargetMetric.ROLLING_CORRECT_ANSWER_PERCENTAGE,
                80.0,
                85.0,
                LearnerStudyPlanTaskDirection.INCREASE,
                LearnerStudyPlanTaskStatus.ACTIVE
        );
        task.setDescription("Improve multiple choice accuracy.");
        studyPlan.getTasks().add(task);

        LearnerStudyPlanStrengthBlock strengthBlock = strengthBlock(
                "strength-1",
                studyPlan,
                1,
                LearnerStudyPlanFocusType.TOPIC,
                null,
                PracticeTopicTag.EDUCATION_AND_LEARNING
        );
        strengthBlock.setDescription("Strong topic performance.");
        strengthBlock.setExplanation("Consistent high accuracy.");
        strengthBlock.setEvidence("90% rolling accuracy.");
        strengthBlock.setRecommendedNextAction("Maintain practice.");
        studyPlan.getStrengthBlocks().add(strengthBlock);

        LearnerStudyPlanWeaknessBlock weaknessBlock = weaknessBlock(
                "weakness-1",
                studyPlan,
                1,
                LearnerStudyPlanFocusType.QUESTION_TYPE,
                PracticeQuestionType.MATCHING,
                null
        );
        weaknessBlock.setDescription("Weak question type performance.");
        weaknessBlock.setExplanation("Accuracy is below target.");
        weaknessBlock.setEvidence("45% rolling accuracy.");
        weaknessBlock.setRecommendedNextAction("Review matching strategies.");
        studyPlan.getWeaknessBlocks().add(weaknessBlock);

        return studyPlan;
    }

    private LearnerStudyPlan studyPlan(
            String id,
            User learner,
            PracticeContentSkill skill
    ) {
        LearnerStudyPlan studyPlan = new LearnerStudyPlan();

        studyPlan.setId(id);
        studyPlan.setUser(learner);
        studyPlan.setSkill(skill);
        studyPlan.setVersionNumber(1);
        studyPlan.setStatus(LearnerStudyPlanStatus.ACTIVE);
        studyPlan.setSubmissionCountUsed(0);
        studyPlan.setSubmissionCountSinceCreation(0);

        return studyPlan;
    }

    private LearnerStudyPlanTask task(
            String id,
            LearnerStudyPlan studyPlan,
            Integer priorityRank,
            LearnerStudyPlanFocusType focusType,
            PracticeQuestionType questionType,
            PracticeTopicTag topicTag,
            LearnerStudyPlanTargetMetric targetMetric,
            Double currentValue,
            Double targetValue,
            LearnerStudyPlanTaskDirection direction,
            LearnerStudyPlanTaskStatus status
    ) {
        LearnerStudyPlanTask task = new LearnerStudyPlanTask();

        task.setId(id);
        task.setLearnerStudyPlan(studyPlan);
        task.setPriorityRank(priorityRank);
        task.setFocusType(focusType);
        task.setQuestionType(questionType);
        task.setTopicTag(topicTag);
        task.setTargetMetric(targetMetric);
        task.setCurrentValue(currentValue);
        task.setTargetValue(targetValue);
        task.setDirection(direction);
        task.setStatus(status);

        return task;
    }

    private LearnerStudyPlanStrengthBlock strengthBlock(
            String id,
            LearnerStudyPlan studyPlan,
            Integer rank,
            LearnerStudyPlanFocusType focusType,
            PracticeQuestionType questionType,
            PracticeTopicTag topicTag
    ) {
        LearnerStudyPlanStrengthBlock strengthBlock = new LearnerStudyPlanStrengthBlock();

        strengthBlock.setId(id);
        strengthBlock.setLearnerStudyPlan(studyPlan);
        strengthBlock.setStrengthRank(rank);
        strengthBlock.setFocusType(focusType);
        strengthBlock.setQuestionType(questionType);
        strengthBlock.setTopicTag(topicTag);

        return strengthBlock;
    }

    private LearnerStudyPlanWeaknessBlock weaknessBlock(
            String id,
            LearnerStudyPlan studyPlan,
            Integer rank,
            LearnerStudyPlanFocusType focusType,
            PracticeQuestionType questionType,
            PracticeTopicTag topicTag
    ) {
        LearnerStudyPlanWeaknessBlock weaknessBlock = new LearnerStudyPlanWeaknessBlock();

        weaknessBlock.setId(id);
        weaknessBlock.setLearnerStudyPlan(studyPlan);
        weaknessBlock.setWeaknessRank(rank);
        weaknessBlock.setFocusType(focusType);
        weaknessBlock.setQuestionType(questionType);
        weaknessBlock.setTopicTag(topicTag);

        return weaknessBlock;
    }

    private void mockIncludes(String... fieldsToInclude) {
        Set<String> includedFields = Set.copyOf(Arrays.asList(fieldsToInclude));

        when(includes.has(anyString())).thenAnswer(invocation -> {
            String fieldName = invocation.getArgument(0);
            return includedFields.contains(fieldName);
        });
    }

    @SuppressWarnings("unchecked")
    private <T> T invokePrivateMethod(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = LearnerStudyPlanServiceImpl.class.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return (T) method.invoke(learnerStudyPlanService, args);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to invoke " + methodName, exception);
        }
    }
}
