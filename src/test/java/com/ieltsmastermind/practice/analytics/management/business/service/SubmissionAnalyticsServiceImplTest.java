package com.ieltsmastermind.practice.analytics.management.business.service;

import com.ieltsmastermind.practice.analytics.management.domain.dto.SubmissionAnalyticsCreateRequestDto;
import com.ieltsmastermind.practice.analytics.management.domain.dto.SubmissionAnalyticsResponseDto;
import com.ieltsmastermind.practice.analytics.management.domain.dto.SubmissionAnalyticsSkillCountResponseDto;
import com.ieltsmastermind.practice.analytics.management.domain.entity.FocusTypeAnalytics;
import com.ieltsmastermind.practice.analytics.management.domain.entity.SubmissionAnalytics;
import com.ieltsmastermind.practice.analytics.management.domain.enums.AnalyticsStrengthLabel;
import com.ieltsmastermind.practice.analytics.management.persistence.SubmissionAnalyticsRepository;
import com.ieltsmastermind.practice.attempt.management.domain.entity.SubmissionQuestionTypeAccuracy;
import com.ieltsmastermind.practice.attempt.management.domain.entity.SubmissionTopicTagAccuracy;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeSubmission;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeWritingAnswer;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeWritingReview;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeSubmissionRepository;
import com.ieltsmastermind.practice.content.management.domain.entity.PracticeContent;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTopicTag;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanFocusType;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanTargetMetric;
import com.ieltsmastermind.user.management.domain.entity.User;
import com.ieltsmastermind.user.management.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
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
class SubmissionAnalyticsServiceImplTest {

    @Mock
    private SubmissionAnalyticsRepository submissionAnalyticsRepository;

    @Mock
    private UserPracticeSubmissionRepository userPracticeSubmissionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SubmissionAnalyticsServiceImpl submissionAnalyticsService;

    @Test
    void create_whenListeningRequestIsValid_shouldCreateListeningSnapshotAndReturnSnapshotId() {
        SubmissionAnalyticsCreateRequestDto request = createRequest(PracticeContentSkill.LISTENING);
        User learner = learner("learner-1");
        SubmissionAnalytics latestSnapshot = analyticsSnapshot("latest-snapshot", learner, PracticeContentSkill.LISTENING);
        latestSnapshot.setVersionNumber(2);

        UserPracticeSubmission submission1 = listeningReadingSubmission(
                "submission-1",
                80.0,
                List.of(
                        questionTypeAccuracy(PracticeQuestionType.MULTIPLE_CHOICE, 10, 8),
                        questionTypeAccuracy(PracticeQuestionType.MATCHING, 5, 2)
                ),
                List.of(
                        topicTagAccuracy(PracticeTopicTag.EDUCATION_AND_LEARNING, 10, 8),
                        topicTagAccuracy(PracticeTopicTag.TECHNOLOGY_INTERNET_AND_AI, 5, 2)
                )
        );
        UserPracticeSubmission submission2 = listeningReadingSubmission(
                "submission-2",
                50.0,
                List.of(
                        questionTypeAccuracy(PracticeQuestionType.MULTIPLE_CHOICE, 10, 4),
                        questionTypeAccuracy(null, 5, 5)
                ),
                List.of(
                        topicTagAccuracy(PracticeTopicTag.EDUCATION_AND_LEARNING, 10, 4),
                        topicTagAccuracy(null, 5, 5)
                )
        );

        when(userRepository.findById("learner-1")).thenReturn(Optional.of(learner));
        when(userPracticeSubmissionRepository.findByUserIdAndPracticeContent_SkillOrderBySubmittedAtAsc(
                "learner-1",
                PracticeContentSkill.LISTENING
        )).thenReturn(List.of(submission1, submission2));
        when(submissionAnalyticsRepository.findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(
                "learner-1",
                PracticeContentSkill.LISTENING
        )).thenReturn(Optional.of(latestSnapshot));
        when(submissionAnalyticsRepository.save(any(SubmissionAnalytics.class))).thenAnswer(invocation -> {
            SubmissionAnalytics snapshot = invocation.getArgument(0);
            snapshot.setId("snapshot-1");
            return snapshot;
        });

        SubmissionAnalyticsResponseDto result = submissionAnalyticsService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("snapshot-1");

        ArgumentCaptor<SubmissionAnalytics> snapshotCaptor =
                ArgumentCaptor.forClass(SubmissionAnalytics.class);
        verify(submissionAnalyticsRepository).save(snapshotCaptor.capture());

        SubmissionAnalytics savedSnapshot = snapshotCaptor.getValue();

        assertThat(savedSnapshot.getUser()).isSameAs(learner);
        assertThat(savedSnapshot.getSkill()).isEqualTo(PracticeContentSkill.LISTENING);
        assertThat(savedSnapshot.getVersionNumber()).isEqualTo(3);
        assertThat(savedSnapshot.getSubmissionCountUsed()).isEqualTo(2);
        assertThat(savedSnapshot.getRollingCorrectAnswerPercentage()).isEqualTo(65.0);
        assertThat(savedSnapshot.getRollingOverallBandScore()).isEqualTo(0.0);
        assertThat(savedSnapshot.getFocusTypeAnalytics()).hasSize(4);

        FocusTypeAnalytics multipleChoiceAnalytics = findQuestionTypeAnalytics(
                savedSnapshot,
                PracticeQuestionType.MULTIPLE_CHOICE
        );
        assertThat(multipleChoiceAnalytics.getSubmissionAnalytics()).isSameAs(savedSnapshot);
        assertThat(multipleChoiceAnalytics.getFocusType()).isEqualTo(LearnerStudyPlanFocusType.QUESTION_TYPE);
        assertThat(multipleChoiceAnalytics.getQuestionType()).isEqualTo(PracticeQuestionType.MULTIPLE_CHOICE);
        assertThat(multipleChoiceAnalytics.getTopicTag()).isNull();
        assertThat(multipleChoiceAnalytics.getContributedSubmissionCount()).isEqualTo(2);
        assertThat(multipleChoiceAnalytics.getRollingExposureCount()).isEqualTo(20);
        assertThat(multipleChoiceAnalytics.getRollingCorrectAnswerPercentage()).isEqualTo(60.0);
        assertThat(multipleChoiceAnalytics.getRollingOverallBandScore()).isEqualTo(0.0);
        assertThat(multipleChoiceAnalytics.getStrengthLabel()).isEqualTo(AnalyticsStrengthLabel.NEUTRAL);

        FocusTypeAnalytics matchingAnalytics = findQuestionTypeAnalytics(
                savedSnapshot,
                PracticeQuestionType.MATCHING
        );
        assertThat(matchingAnalytics.getContributedSubmissionCount()).isEqualTo(1);
        assertThat(matchingAnalytics.getRollingExposureCount()).isEqualTo(5);
        assertThat(matchingAnalytics.getRollingCorrectAnswerPercentage()).isEqualTo(40.0);
        assertThat(matchingAnalytics.getStrengthLabel()).isEqualTo(AnalyticsStrengthLabel.WEAK);

        FocusTypeAnalytics educationAnalytics = findTopicAnalytics(
                savedSnapshot,
                PracticeTopicTag.EDUCATION_AND_LEARNING
        );
        assertThat(educationAnalytics.getSubmissionAnalytics()).isSameAs(savedSnapshot);
        assertThat(educationAnalytics.getFocusType()).isEqualTo(LearnerStudyPlanFocusType.TOPIC);
        assertThat(educationAnalytics.getQuestionType()).isNull();
        assertThat(educationAnalytics.getTopicTag()).isEqualTo(PracticeTopicTag.EDUCATION_AND_LEARNING);
        assertThat(educationAnalytics.getContributedSubmissionCount()).isEqualTo(2);
        assertThat(educationAnalytics.getRollingExposureCount()).isEqualTo(20);
        assertThat(educationAnalytics.getRollingCorrectAnswerPercentage()).isEqualTo(60.0);
        assertThat(educationAnalytics.getStrengthLabel()).isEqualTo(AnalyticsStrengthLabel.NEUTRAL);

        FocusTypeAnalytics technologyAnalytics = findTopicAnalytics(
                savedSnapshot,
                PracticeTopicTag.TECHNOLOGY_INTERNET_AND_AI
        );
        assertThat(technologyAnalytics.getContributedSubmissionCount()).isEqualTo(1);
        assertThat(technologyAnalytics.getRollingExposureCount()).isEqualTo(5);
        assertThat(technologyAnalytics.getRollingCorrectAnswerPercentage()).isEqualTo(40.0);
        assertThat(technologyAnalytics.getStrengthLabel()).isEqualTo(AnalyticsStrengthLabel.WEAK);

        verify(userRepository).findById("learner-1");
        verify(userPracticeSubmissionRepository).findByUserIdAndPracticeContent_SkillOrderBySubmittedAtAsc(
                "learner-1",
                PracticeContentSkill.LISTENING
        );
    }

    @Test
    void createSnapshot_whenReadingSubmissionsHaveZeroExposure_shouldCreateWeakZeroPercentageAnalytics() {
        User learner = learner("learner-1");
        UserPracticeSubmission submission = listeningReadingSubmission(
                "submission-1",
                null,
                List.of(questionTypeAccuracy(PracticeQuestionType.MULTIPLE_CHOICE, null, null)),
                List.of(topicTagAccuracy(PracticeTopicTag.EDUCATION_AND_LEARNING, null, null))
        );

        when(userRepository.findById("learner-1")).thenReturn(Optional.of(learner));
        when(userPracticeSubmissionRepository.findByUserIdAndPracticeContent_SkillOrderBySubmittedAtAsc(
                "learner-1",
                PracticeContentSkill.READING
        )).thenReturn(List.of(submission));
        when(submissionAnalyticsRepository.findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(
                "learner-1",
                PracticeContentSkill.READING
        )).thenReturn(Optional.empty());
        when(submissionAnalyticsRepository.save(any(SubmissionAnalytics.class))).thenAnswer(invocation -> {
            SubmissionAnalytics snapshot = invocation.getArgument(0);
            snapshot.setId("snapshot-1");
            return snapshot;
        });

        SubmissionAnalytics result =
                submissionAnalyticsService.createSnapshot("learner-1", PracticeContentSkill.READING);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("snapshot-1");
        assertThat(result.getVersionNumber()).isEqualTo(1);
        assertThat(result.getRollingCorrectAnswerPercentage()).isEqualTo(0.0);
        assertThat(result.getFocusTypeAnalytics()).hasSize(2);

        FocusTypeAnalytics questionTypeAnalytics =
                findQuestionTypeAnalytics(result, PracticeQuestionType.MULTIPLE_CHOICE);
        assertThat(questionTypeAnalytics.getRollingExposureCount()).isZero();
        assertThat(questionTypeAnalytics.getRollingCorrectAnswerPercentage()).isEqualTo(0.0);
        assertThat(questionTypeAnalytics.getStrengthLabel()).isEqualTo(AnalyticsStrengthLabel.WEAK);

        FocusTypeAnalytics topicAnalytics =
                findTopicAnalytics(result, PracticeTopicTag.EDUCATION_AND_LEARNING);
        assertThat(topicAnalytics.getRollingExposureCount()).isZero();
        assertThat(topicAnalytics.getRollingCorrectAnswerPercentage()).isEqualTo(0.0);
        assertThat(topicAnalytics.getStrengthLabel()).isEqualTo(AnalyticsStrengthLabel.WEAK);
    }

    @Test
    void createSnapshot_whenWritingSubmissionsHaveReviews_shouldCreateWritingSnapshotWithBandAnalytics() {
        User learner = learner("learner-1");

        UserPracticeSubmission strongSubmission = writingSubmission(
                "submission-1",
                8.0,
                PracticeQuestionType.OPINION,
                PracticeTopicTag.WORK_JOBS_AND_CAREERS
        );
        UserPracticeSubmission neutralSubmission = writingSubmission(
                "submission-2",
                6.0,
                PracticeQuestionType.OPINION,
                PracticeTopicTag.WORK_JOBS_AND_CAREERS
        );
        UserPracticeSubmission weakSubmission = writingSubmission(
                "submission-3",
                5.0,
                PracticeQuestionType.PROBLEM_SOLUTION,
                PracticeTopicTag.EDUCATION_AND_LEARNING
        );
        UserPracticeSubmission unreviewedSubmission = submissionWithoutReview("submission-4");

        SubmissionAnalytics latestSnapshot = analyticsSnapshot("latest-snapshot", learner, PracticeContentSkill.WRITING);
        latestSnapshot.setVersionNumber(4);

        when(userRepository.findById("learner-1")).thenReturn(Optional.of(learner));
        when(userPracticeSubmissionRepository.findByUserIdAndPracticeContent_SkillOrderBySubmittedAtAsc(
                "learner-1",
                PracticeContentSkill.WRITING
        )).thenReturn(List.of(strongSubmission, neutralSubmission, weakSubmission, unreviewedSubmission));
        when(submissionAnalyticsRepository.findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(
                "learner-1",
                PracticeContentSkill.WRITING
        )).thenReturn(Optional.of(latestSnapshot));
        when(submissionAnalyticsRepository.save(any(SubmissionAnalytics.class))).thenAnswer(invocation -> {
            SubmissionAnalytics snapshot = invocation.getArgument(0);
            snapshot.setId("snapshot-1");
            return snapshot;
        });

        SubmissionAnalytics result =
                submissionAnalyticsService.createSnapshot("learner-1", PracticeContentSkill.WRITING);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("snapshot-1");
        assertThat(result.getUser()).isSameAs(learner);
        assertThat(result.getSkill()).isEqualTo(PracticeContentSkill.WRITING);
        assertThat(result.getVersionNumber()).isEqualTo(5);
        assertThat(result.getSubmissionCountUsed()).isEqualTo(3);
        assertThat(result.getRollingOverallBandScore()).isEqualTo(6.3);
        assertThat(result.getRollingCorrectAnswerPercentage()).isEqualTo(0.0);
        assertThat(result.getFocusTypeAnalytics()).hasSize(4);

        FocusTypeAnalytics opinionAnalytics = findQuestionTypeAnalytics(result, PracticeQuestionType.OPINION);
        assertThat(opinionAnalytics.getContributedSubmissionCount()).isEqualTo(2);
        assertThat(opinionAnalytics.getRollingExposureCount()).isEqualTo(2);
        assertThat(opinionAnalytics.getRollingOverallBandScore()).isEqualTo(7.0);
        assertThat(opinionAnalytics.getRollingCorrectAnswerPercentage()).isEqualTo(0.0);
        assertThat(opinionAnalytics.getStrengthLabel()).isEqualTo(AnalyticsStrengthLabel.NEUTRAL);

        FocusTypeAnalytics problemSolutionAnalytics = findQuestionTypeAnalytics(result, PracticeQuestionType.PROBLEM_SOLUTION);
        assertThat(problemSolutionAnalytics.getContributedSubmissionCount()).isEqualTo(1);
        assertThat(problemSolutionAnalytics.getRollingExposureCount()).isEqualTo(1);
        assertThat(problemSolutionAnalytics.getRollingOverallBandScore()).isEqualTo(5.0);
        assertThat(problemSolutionAnalytics.getStrengthLabel()).isEqualTo(AnalyticsStrengthLabel.WEAK);

        FocusTypeAnalytics workAnalytics = findTopicAnalytics(result, PracticeTopicTag.WORK_JOBS_AND_CAREERS);
        assertThat(workAnalytics.getContributedSubmissionCount()).isEqualTo(2);
        assertThat(workAnalytics.getRollingExposureCount()).isEqualTo(2);
        assertThat(workAnalytics.getRollingOverallBandScore()).isEqualTo(7.0);
        assertThat(workAnalytics.getStrengthLabel()).isEqualTo(AnalyticsStrengthLabel.NEUTRAL);

        FocusTypeAnalytics educationAnalytics = findTopicAnalytics(result, PracticeTopicTag.EDUCATION_AND_LEARNING);
        assertThat(educationAnalytics.getContributedSubmissionCount()).isEqualTo(1);
        assertThat(educationAnalytics.getRollingExposureCount()).isEqualTo(1);
        assertThat(educationAnalytics.getRollingOverallBandScore()).isEqualTo(5.0);
        assertThat(educationAnalytics.getStrengthLabel()).isEqualTo(AnalyticsStrengthLabel.WEAK);

        verify(submissionAnalyticsRepository).save(any(SubmissionAnalytics.class));
    }

    @Test
    void createSnapshot_whenUserDoesNotExist_shouldThrowIllegalArgumentExceptionAndSkipSubmissionLookup() {
        when(userRepository.findById("missing-user")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> submissionAnalyticsService.createSnapshot("missing-user", PracticeContentSkill.LISTENING)
        );

        assertThat(exception.getMessage()).isEqualTo("User not found: missing-user");

        verify(userRepository).findById("missing-user");
        verify(userPracticeSubmissionRepository, never())
                .findByUserIdAndPracticeContent_SkillOrderBySubmittedAtAsc(anyString(), any(PracticeContentSkill.class));
        verify(submissionAnalyticsRepository, never()).save(any(SubmissionAnalytics.class));
    }

    @Test
    void createSnapshot_whenListeningSubmissionsAreEmpty_shouldThrowIllegalArgumentExceptionAndSkipSave() {
        User learner = learner("learner-1");

        when(userRepository.findById("learner-1")).thenReturn(Optional.of(learner));
        when(userPracticeSubmissionRepository.findByUserIdAndPracticeContent_SkillOrderBySubmittedAtAsc(
                "learner-1",
                PracticeContentSkill.LISTENING
        )).thenReturn(List.of());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> submissionAnalyticsService.createSnapshot("learner-1", PracticeContentSkill.LISTENING)
        );

        assertThat(exception.getMessage())
                .isEqualTo("No submissions found for learner: learner-1 with skill: LISTENING");

        verify(submissionAnalyticsRepository, never()).save(any(SubmissionAnalytics.class));
    }

    @Test
    void createSnapshot_whenWritingSubmissionsHaveNoReviews_shouldThrowIllegalArgumentExceptionAndSkipSave() {
        User learner = learner("learner-1");

        when(userRepository.findById("learner-1")).thenReturn(Optional.of(learner));
        when(userPracticeSubmissionRepository.findByUserIdAndPracticeContent_SkillOrderBySubmittedAtAsc(
                "learner-1",
                PracticeContentSkill.WRITING
        )).thenReturn(List.of(submissionWithoutReview("submission-1")));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> submissionAnalyticsService.createSnapshot("learner-1", PracticeContentSkill.WRITING)
        );

        assertThat(exception.getMessage())
                .isEqualTo("No reviewed writing submissions found for learner: learner-1");

        verify(submissionAnalyticsRepository, never()).save(any(SubmissionAnalytics.class));
    }

    @Test
    void createSnapshot_whenWritingSubmissionHasNoPracticeContent_shouldThrowIllegalArgumentExceptionAndSkipSave() {
        User learner = learner("learner-1");
        UserPracticeSubmission submission = writingSubmission(
                "submission-1",
                7.5,
                PracticeQuestionType.OPINION,
                PracticeTopicTag.WORK_JOBS_AND_CAREERS
        );
        submission.setPracticeContent(null);

        when(userRepository.findById("learner-1")).thenReturn(Optional.of(learner));
        when(userPracticeSubmissionRepository.findByUserIdAndPracticeContent_SkillOrderBySubmittedAtAsc(
                "learner-1",
                PracticeContentSkill.WRITING
        )).thenReturn(List.of(submission));
        when(submissionAnalyticsRepository.findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(
                "learner-1",
                PracticeContentSkill.WRITING
        )).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> submissionAnalyticsService.createSnapshot("learner-1", PracticeContentSkill.WRITING)
        );

        assertThat(exception.getMessage()).isEqualTo("No practice content found for submission: submission-1");

        verify(submissionAnalyticsRepository, never()).save(any(SubmissionAnalytics.class));
    }

    @Test
    void createSnapshot_whenWritingPracticeContentHasNoQuestionTypeTag_shouldThrowIllegalArgumentExceptionAndSkipSave() {
        User learner = learner("learner-1");
        UserPracticeSubmission submission = writingSubmission(
                "submission-1",
                7.5,
                PracticeQuestionType.OPINION,
                PracticeTopicTag.WORK_JOBS_AND_CAREERS
        );
        submission.getPracticeContent().setQuestionTypeTags(new LinkedHashSet<>());

        when(userRepository.findById("learner-1")).thenReturn(Optional.of(learner));
        when(userPracticeSubmissionRepository.findByUserIdAndPracticeContent_SkillOrderBySubmittedAtAsc(
                "learner-1",
                PracticeContentSkill.WRITING
        )).thenReturn(List.of(submission));
        when(submissionAnalyticsRepository.findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(
                "learner-1",
                PracticeContentSkill.WRITING
        )).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> submissionAnalyticsService.createSnapshot("learner-1", PracticeContentSkill.WRITING)
        );

        assertThat(exception.getMessage()).isEqualTo("No question type tag found for practice content: content-submission-1");

        verify(submissionAnalyticsRepository, never()).save(any(SubmissionAnalytics.class));
    }

    @Test
    void createSnapshot_whenWritingPracticeContentHasNoTopicTag_shouldThrowIllegalArgumentExceptionAndSkipSave() {
        User learner = learner("learner-1");
        UserPracticeSubmission submission = writingSubmission(
                "submission-1",
                7.5,
                PracticeQuestionType.OPINION,
                PracticeTopicTag.WORK_JOBS_AND_CAREERS
        );
        submission.getPracticeContent().setTopicTags(new LinkedHashSet<>());

        when(userRepository.findById("learner-1")).thenReturn(Optional.of(learner));
        when(userPracticeSubmissionRepository.findByUserIdAndPracticeContent_SkillOrderBySubmittedAtAsc(
                "learner-1",
                PracticeContentSkill.WRITING
        )).thenReturn(List.of(submission));
        when(submissionAnalyticsRepository.findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(
                "learner-1",
                PracticeContentSkill.WRITING
        )).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> submissionAnalyticsService.createSnapshot("learner-1", PracticeContentSkill.WRITING)
        );

        assertThat(exception.getMessage()).isEqualTo("No topic tag found for practice content: content-submission-1");

        verify(submissionAnalyticsRepository, never()).save(any(SubmissionAnalytics.class));
    }

    @Test
    void getCurrentValue_whenTargetMetricIsCorrectAnswerPercentageAndFocusTypeIsNull_shouldReturnSnapshotRollingPercentage() {
        SubmissionAnalytics snapshot = analyticsSnapshot("snapshot-1", learner("learner-1"), PracticeContentSkill.LISTENING);
        snapshot.setRollingCorrectAnswerPercentage(67.5);

        when(submissionAnalyticsRepository.findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(
                "learner-1",
                PracticeContentSkill.LISTENING
        )).thenReturn(Optional.of(snapshot));

        Double result = submissionAnalyticsService.getCurrentValue(
                null,
                LearnerStudyPlanTargetMetric.ROLLING_CORRECT_ANSWER_PERCENTAGE,
                "learner-1",
                PracticeContentSkill.LISTENING,
                null,
                null
        );

        assertThat(result).isEqualTo(67.5);
    }

    @Test
    void getCurrentValue_whenTargetMetricIsOverallBandScoreAndFocusTypeIsNull_shouldReturnSnapshotRollingBandScore() {
        SubmissionAnalytics snapshot = analyticsSnapshot("snapshot-1", learner("learner-1"), PracticeContentSkill.WRITING);
        snapshot.setRollingOverallBandScore(7.5);

        when(submissionAnalyticsRepository.findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(
                "learner-1",
                PracticeContentSkill.WRITING
        )).thenReturn(Optional.of(snapshot));

        Double result = submissionAnalyticsService.getCurrentValue(
                null,
                LearnerStudyPlanTargetMetric.ROLLING_OVERALL_BAND_SCORE,
                "learner-1",
                PracticeContentSkill.WRITING,
                null,
                null
        );

        assertThat(result).isEqualTo(7.5);
    }

    @Test
    void getCurrentValue_whenQuestionTypeFocusAndPercentageMetric_shouldReturnQuestionTypePercentage() {
        SubmissionAnalytics snapshot = analyticsSnapshot("snapshot-1", learner("learner-1"), PracticeContentSkill.LISTENING);
        snapshot.getFocusTypeAnalytics().add(questionTypeFocusAnalytics(
                snapshot,
                PracticeQuestionType.MULTIPLE_CHOICE,
                80.0,
                0.0,
                10,
                AnalyticsStrengthLabel.STRONG
        ));

        when(submissionAnalyticsRepository.findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(
                "learner-1",
                PracticeContentSkill.LISTENING
        )).thenReturn(Optional.of(snapshot));

        Double result = submissionAnalyticsService.getCurrentValue(
                LearnerStudyPlanFocusType.QUESTION_TYPE,
                LearnerStudyPlanTargetMetric.ROLLING_CORRECT_ANSWER_PERCENTAGE,
                "learner-1",
                PracticeContentSkill.LISTENING,
                PracticeQuestionType.MULTIPLE_CHOICE,
                null
        );

        assertThat(result).isEqualTo(80.0);
    }

    @Test
    void getCurrentValue_whenTopicFocusAndBandScoreMetric_shouldReturnTopicBandScore() {
        SubmissionAnalytics snapshot = analyticsSnapshot("snapshot-1", learner("learner-1"), PracticeContentSkill.WRITING);
        snapshot.getFocusTypeAnalytics().add(topicFocusAnalytics(
                snapshot,
                PracticeTopicTag.WORK_JOBS_AND_CAREERS,
                0.0,
                7.5,
                2,
                AnalyticsStrengthLabel.STRONG
        ));

        when(submissionAnalyticsRepository.findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(
                "learner-1",
                PracticeContentSkill.WRITING
        )).thenReturn(Optional.of(snapshot));

        Double result = submissionAnalyticsService.getCurrentValue(
                LearnerStudyPlanFocusType.TOPIC,
                LearnerStudyPlanTargetMetric.ROLLING_OVERALL_BAND_SCORE,
                "learner-1",
                PracticeContentSkill.WRITING,
                null,
                PracticeTopicTag.WORK_JOBS_AND_CAREERS
        );

        assertThat(result).isEqualTo(7.5);
    }

    @Test
    void getCurrentValue_whenNoSnapshotExists_shouldThrowIllegalArgumentException() {
        when(submissionAnalyticsRepository.findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(
                "learner-1",
                PracticeContentSkill.LISTENING
        )).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> submissionAnalyticsService.getCurrentValue(
                        null,
                        LearnerStudyPlanTargetMetric.ROLLING_CORRECT_ANSWER_PERCENTAGE,
                        "learner-1",
                        PracticeContentSkill.LISTENING,
                        null,
                        null
                )
        );

        assertThat(exception.getMessage())
                .isEqualTo("No submission analytics found for user: learner-1 and skill: LISTENING");
    }

    @Test
    void getCurrentValue_whenTargetMetricIsSkipRate_shouldThrowUnsupportedMetricException() {
        SubmissionAnalytics snapshot = analyticsSnapshot("snapshot-1", learner("learner-1"), PracticeContentSkill.LISTENING);

        when(submissionAnalyticsRepository.findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(
                "learner-1",
                PracticeContentSkill.LISTENING
        )).thenReturn(Optional.of(snapshot));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> submissionAnalyticsService.getCurrentValue(
                        null,
                        LearnerStudyPlanTargetMetric.ROLLING_SKIP_RATE,
                        "learner-1",
                        PracticeContentSkill.LISTENING,
                        null,
                        null
                )
        );

        assertThat(exception.getMessage()).isEqualTo("ROLLING_SKIP_RATE is not supported by SubmissionAnalytics");
    }

    @Test
    void getCurrentValue_whenTargetMetricIsEffectiveAccuracy_shouldThrowUnsupportedMetricException() {
        SubmissionAnalytics snapshot = analyticsSnapshot("snapshot-1", learner("learner-1"), PracticeContentSkill.LISTENING);

        when(submissionAnalyticsRepository.findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(
                "learner-1",
                PracticeContentSkill.LISTENING
        )).thenReturn(Optional.of(snapshot));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> submissionAnalyticsService.getCurrentValue(
                        null,
                        LearnerStudyPlanTargetMetric.ROLLING_EFFECTIVE_ACCURACY,
                        "learner-1",
                        PracticeContentSkill.LISTENING,
                        null,
                        null
                )
        );

        assertThat(exception.getMessage()).isEqualTo("ROLLING_EFFECTIVE_ACCURACY is not supported by SubmissionAnalytics");
    }

    @Test
    void getCurrentValue_whenQuestionTypeFocusHasNullQuestionType_shouldThrowIllegalArgumentException() {
        SubmissionAnalytics snapshot = analyticsSnapshot("snapshot-1", learner("learner-1"), PracticeContentSkill.LISTENING);

        when(submissionAnalyticsRepository.findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(
                "learner-1",
                PracticeContentSkill.LISTENING
        )).thenReturn(Optional.of(snapshot));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> submissionAnalyticsService.getCurrentValue(
                        LearnerStudyPlanFocusType.QUESTION_TYPE,
                        LearnerStudyPlanTargetMetric.ROLLING_CORRECT_ANSWER_PERCENTAGE,
                        "learner-1",
                        PracticeContentSkill.LISTENING,
                        null,
                        null
                )
        );

        assertThat(exception.getMessage())
                .isEqualTo("Question type is required when focus type is QUESTION_TYPE");
    }

    @Test
    void getCurrentValue_whenTopicFocusHasNullTopicTag_shouldThrowIllegalArgumentException() {
        SubmissionAnalytics snapshot = analyticsSnapshot("snapshot-1", learner("learner-1"), PracticeContentSkill.LISTENING);

        when(submissionAnalyticsRepository.findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(
                "learner-1",
                PracticeContentSkill.LISTENING
        )).thenReturn(Optional.of(snapshot));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> submissionAnalyticsService.getCurrentValue(
                        LearnerStudyPlanFocusType.TOPIC,
                        LearnerStudyPlanTargetMetric.ROLLING_CORRECT_ANSWER_PERCENTAGE,
                        "learner-1",
                        PracticeContentSkill.LISTENING,
                        null,
                        null
                )
        );

        assertThat(exception.getMessage())
                .isEqualTo("Topic tag is required when focus type is TOPIC");
    }

    @Test
    void getCurrentValue_whenQuestionTypeAnalyticsDoesNotExist_shouldThrowIllegalArgumentException() {
        SubmissionAnalytics snapshot = analyticsSnapshot("snapshot-1", learner("learner-1"), PracticeContentSkill.LISTENING);

        when(submissionAnalyticsRepository.findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(
                "learner-1",
                PracticeContentSkill.LISTENING
        )).thenReturn(Optional.of(snapshot));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> submissionAnalyticsService.getCurrentValue(
                        LearnerStudyPlanFocusType.QUESTION_TYPE,
                        LearnerStudyPlanTargetMetric.ROLLING_CORRECT_ANSWER_PERCENTAGE,
                        "learner-1",
                        PracticeContentSkill.LISTENING,
                        PracticeQuestionType.MULTIPLE_CHOICE,
                        null
                )
        );

        assertThat(exception.getMessage())
                .isEqualTo("No question type analytics found for question type: MULTIPLE_CHOICE");
    }

    @Test
    void getCurrentValue_whenTopicAnalyticsDoesNotExist_shouldThrowIllegalArgumentException() {
        SubmissionAnalytics snapshot = analyticsSnapshot("snapshot-1", learner("learner-1"), PracticeContentSkill.LISTENING);

        when(submissionAnalyticsRepository.findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(
                "learner-1",
                PracticeContentSkill.LISTENING
        )).thenReturn(Optional.of(snapshot));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> submissionAnalyticsService.getCurrentValue(
                        LearnerStudyPlanFocusType.TOPIC,
                        LearnerStudyPlanTargetMetric.ROLLING_CORRECT_ANSWER_PERCENTAGE,
                        "learner-1",
                        PracticeContentSkill.LISTENING,
                        null,
                        PracticeTopicTag.EDUCATION_AND_LEARNING
                )
        );

        assertThat(exception.getMessage())
                .isEqualTo("No topic analytics found for topic tag: EDUCATION_AND_LEARNING");
    }

    @Test
    void getAnalyticsSubmissionCountsByUserId_whenUserExists_shouldReturnCountsForEachSkill() {
        String userId = "learner-1";
        User learner = learner(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(learner));
        when(userPracticeSubmissionRepository.findByUserIdAndPracticeContent_SkillOrderBySubmittedAtAsc(
                userId,
                PracticeContentSkill.LISTENING
        )).thenReturn(List.of(
                listeningReadingSubmission("listening-1", 70.0, List.of(), List.of()),
                listeningReadingSubmission("listening-2", 80.0, List.of(), List.of())
        ));
        when(userPracticeSubmissionRepository.findByUserIdAndPracticeContent_SkillOrderBySubmittedAtAsc(
                userId,
                PracticeContentSkill.READING
        )).thenReturn(List.of(
                listeningReadingSubmission("reading-1", 60.0, List.of(), List.of())
        ));
        when(userPracticeSubmissionRepository.findByUserIdAndPracticeContent_SkillOrderBySubmittedAtAsc(
                userId,
                PracticeContentSkill.WRITING
        )).thenReturn(List.of(
                writingSubmission("writing-1", 7.0, PracticeQuestionType.OPINION, PracticeTopicTag.WORK_JOBS_AND_CAREERS),
                submissionWithoutReview("writing-2")
        ));
        when(userPracticeSubmissionRepository.findByUserIdAndPracticeContent_SkillOrderBySubmittedAtAsc(
                userId,
                PracticeContentSkill.SPEAKING
        )).thenReturn(List.of());

        SubmissionAnalyticsSkillCountResponseDto result =
                submissionAnalyticsService.getAnalyticsSubmissionCountsByUserId(userId);

        assertThat(result).isNotNull();
        assertThat(result.getListeningCount()).isEqualTo(2L);
        assertThat(result.getReadingCount()).isEqualTo(1L);
        assertThat(result.getWritingCount()).isEqualTo(1L);
        assertThat(result.getSpeakingCount()).isZero();

        verify(userRepository).findById(userId);
    }

    @Test
    void getAnalyticsSubmissionCountsByUserId_whenUserDoesNotExist_shouldThrowIllegalArgumentExceptionAndSkipSubmissionLookup() {
        when(userRepository.findById("missing-user")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> submissionAnalyticsService.getAnalyticsSubmissionCountsByUserId("missing-user")
        );

        assertThat(exception.getMessage()).isEqualTo("User not found: missing-user");

        verify(userPracticeSubmissionRepository, never())
                .findByUserIdAndPracticeContent_SkillOrderBySubmittedAtAsc(anyString(), any(PracticeContentSkill.class));
    }

    @Test
    void safeHelperMethods_whenValuesAreNullOrDecimal_shouldReturnExpectedValues() {
        Integer safeIntResult = invokePrivateMethod("safeInt", new Class<?>[]{Integer.class}, new Object[]{null});
        Double safeDoubleResult = invokePrivateMethod("safeDouble", new Class<?>[]{Double.class}, new Object[]{null});
        Double roundResult = invokePrivateMethod("roundToOneDecimal", new Class<?>[]{double.class}, 66.66);

        assertThat(safeIntResult).isZero();
        assertThat(safeDoubleResult).isEqualTo(0.0);
        assertThat(roundResult).isEqualTo(66.7);
    }

    private SubmissionAnalyticsCreateRequestDto createRequest(PracticeContentSkill skill) {
        SubmissionAnalyticsCreateRequestDto request = new SubmissionAnalyticsCreateRequestDto();

        request.setLearnerId("learner-1");
        request.setSkill(skill);

        return request;
    }

    private User learner(String userId) {
        User user = new User();

        user.setUserId(userId);
        user.setEmail(userId + "@example.com");
        user.setFirstname("Learner");
        user.setLastname("User");

        return user;
    }

    private SubmissionAnalytics analyticsSnapshot(String id, User learner, PracticeContentSkill skill) {
        SubmissionAnalytics snapshot = new SubmissionAnalytics();

        snapshot.setId(id);
        snapshot.setUser(learner);
        snapshot.setSkill(skill);
        snapshot.setVersionNumber(1);
        snapshot.setSubmissionCountUsed(1);
        snapshot.setRollingCorrectAnswerPercentage(0.0);
        snapshot.setRollingOverallBandScore(0.0);
        snapshot.setCalculatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        snapshot.setUpdatedAt(LocalDateTime.of(2026, 1, 2, 10, 0));
        snapshot.setFocusTypeAnalytics(new LinkedHashSet<>());

        return snapshot;
    }

    private UserPracticeSubmission listeningReadingSubmission(
            String id,
            Double accuracyRate,
            List<SubmissionQuestionTypeAccuracy> questionTypeAccuracies,
            List<SubmissionTopicTagAccuracy> topicTagAccuracies
    ) {
        UserPracticeSubmission submission = new UserPracticeSubmission();

        submission.setId(id);
        submission.setAccuracyRate(accuracyRate);
        submission.setQuestionTypeAccuracies(questionTypeAccuracies);
        submission.setTopicTagAccuracies(topicTagAccuracies);

        return submission;
    }

    private UserPracticeSubmission writingSubmission(
            String id,
            Double overallTutorBand,
            PracticeQuestionType questionType,
            PracticeTopicTag topicTag
    ) {
        UserPracticeSubmission submission = new UserPracticeSubmission();

        submission.setId(id);
        submission.setPracticeContent(practiceContent("content-" + id, questionType, topicTag));

        UserPracticeWritingAnswer writingAnswer = new UserPracticeWritingAnswer();
        writingAnswer.setId("writing-answer-" + id);

        UserPracticeWritingReview writingReview = new UserPracticeWritingReview();
        writingReview.setId("review-" + id);
        writingReview.setOverallTutorBand(overallTutorBand);

        writingAnswer.getWritingReviews().add(writingReview);
        submission.getWritingAnswers().add(writingAnswer);

        return submission;
    }

    private UserPracticeSubmission submissionWithoutReview(String id) {
        UserPracticeSubmission submission = new UserPracticeSubmission();

        submission.setId(id);
        submission.setPracticeContent(
                practiceContent(
                        "content-" + id,
                        PracticeQuestionType.OPINION,
                        PracticeTopicTag.WORK_JOBS_AND_CAREERS
                )
        );

        UserPracticeWritingAnswer writingAnswer = new UserPracticeWritingAnswer();
        writingAnswer.setId("writing-answer-" + id);

        submission.getWritingAnswers().add(writingAnswer);

        return submission;
    }

    private PracticeContent practiceContent(
            String id,
            PracticeQuestionType questionType,
            PracticeTopicTag topicTag
    ) {
        PracticeContent practiceContent = new PracticeContent();

        setIfPresent(practiceContent, "setId", id);
        practiceContent.setSkill(PracticeContentSkill.WRITING);
        practiceContent.setQuestionTypeTags(new LinkedHashSet<>(Set.of(questionType)));
        practiceContent.setTopicTags(new LinkedHashSet<>(Set.of(topicTag)));

        return practiceContent;
    }

    private SubmissionQuestionTypeAccuracy questionTypeAccuracy(
            PracticeQuestionType questionType,
            Integer exposureCount,
            Integer correctQuestionCount
    ) {
        SubmissionQuestionTypeAccuracy accuracy = new SubmissionQuestionTypeAccuracy();

        accuracy.setQuestionType(questionType);
        accuracy.setExposureCount(exposureCount);
        accuracy.setCorrectQuestionCount(correctQuestionCount);

        return accuracy;
    }

    private SubmissionTopicTagAccuracy topicTagAccuracy(
            PracticeTopicTag topicTag,
            Integer exposureCount,
            Integer correctQuestionCount
    ) {
        SubmissionTopicTagAccuracy accuracy = new SubmissionTopicTagAccuracy();

        accuracy.setTopicTag(topicTag);
        accuracy.setExposureCount(exposureCount);
        accuracy.setCorrectQuestionCount(correctQuestionCount);

        return accuracy;
    }

    private FocusTypeAnalytics questionTypeFocusAnalytics(
            SubmissionAnalytics snapshot,
            PracticeQuestionType questionType,
            Double rollingCorrectAnswerPercentage,
            Double rollingOverallBandScore,
            Integer rollingExposureCount,
            AnalyticsStrengthLabel strengthLabel
    ) {
        FocusTypeAnalytics analytics = new FocusTypeAnalytics();

        analytics.setSubmissionAnalytics(snapshot);
        analytics.setFocusType(LearnerStudyPlanFocusType.QUESTION_TYPE);
        analytics.setQuestionType(questionType);
        analytics.setTopicTag(null);
        analytics.setContributedSubmissionCount(1);
        analytics.setRollingExposureCount(rollingExposureCount);
        analytics.setRollingCorrectAnswerPercentage(rollingCorrectAnswerPercentage);
        analytics.setRollingOverallBandScore(rollingOverallBandScore);
        analytics.setStrengthLabel(strengthLabel);

        return analytics;
    }

    private FocusTypeAnalytics topicFocusAnalytics(
            SubmissionAnalytics snapshot,
            PracticeTopicTag topicTag,
            Double rollingCorrectAnswerPercentage,
            Double rollingOverallBandScore,
            Integer rollingExposureCount,
            AnalyticsStrengthLabel strengthLabel
    ) {
        FocusTypeAnalytics analytics = new FocusTypeAnalytics();

        analytics.setSubmissionAnalytics(snapshot);
        analytics.setFocusType(LearnerStudyPlanFocusType.TOPIC);
        analytics.setQuestionType(null);
        analytics.setTopicTag(topicTag);
        analytics.setContributedSubmissionCount(1);
        analytics.setRollingExposureCount(rollingExposureCount);
        analytics.setRollingCorrectAnswerPercentage(rollingCorrectAnswerPercentage);
        analytics.setRollingOverallBandScore(rollingOverallBandScore);
        analytics.setStrengthLabel(strengthLabel);

        return analytics;
    }

    private FocusTypeAnalytics findQuestionTypeAnalytics(
            SubmissionAnalytics snapshot,
            PracticeQuestionType questionType
    ) {
        return snapshot.getFocusTypeAnalytics()
                .stream()
                .filter(analytics -> analytics.getFocusType() == LearnerStudyPlanFocusType.QUESTION_TYPE)
                .filter(analytics -> analytics.getQuestionType() == questionType)
                .findFirst()
                .orElseThrow();
    }

    private FocusTypeAnalytics findTopicAnalytics(
            SubmissionAnalytics snapshot,
            PracticeTopicTag topicTag
    ) {
        return snapshot.getFocusTypeAnalytics()
                .stream()
                .filter(analytics -> analytics.getFocusType() == LearnerStudyPlanFocusType.TOPIC)
                .filter(analytics -> analytics.getTopicTag() == topicTag)
                .findFirst()
                .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private <T> T invokePrivateMethod(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = SubmissionAnalyticsServiceImpl.class.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return (T) method.invoke(submissionAnalyticsService, args);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to invoke " + methodName, exception);
        }
    }

    private void setIfPresent(Object target, String setterName, Object value) {
        Method setter = java.util.Arrays.stream(target.getClass().getMethods())
                .filter(method -> method.getName().equals(setterName))
                .filter(method -> method.getParameterCount() == 1)
                .filter(method -> value == null || method.getParameterTypes()[0].isAssignableFrom(value.getClass()))
                .findFirst()
                .orElse(null);

        if (setter == null) {
            return;
        }

        try {
            setter.invoke(target, value);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to call " + setterName, exception);
        }
    }
}
