package com.ieltsmastermind.practice.attempt.management.business.service;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeSubmissionAnswerBulkCreateRequestDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeSubmissionAnswerCreateRequestDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeSubmissionAnswerResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.entity.SubmissionQuestionTypeAccuracy;
import com.ieltsmastermind.practice.attempt.management.domain.entity.SubmissionTopicTagAccuracy;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeSubmission;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeSubmissionAnswer;
import com.ieltsmastermind.practice.attempt.management.domain.enums.Result;
import com.ieltsmastermind.practice.attempt.management.persistence.SubmissionQuestionTypeAccuracyRepository;
import com.ieltsmastermind.practice.attempt.management.persistence.SubmissionTopicTagAccuracyRepository;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeSubmissionAnswerRepository;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeSubmissionRepository;
import com.ieltsmastermind.practice.content.management.domain.entity.PracticeQuestion;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTopicTag;
import com.ieltsmastermind.practice.content.management.persistence.PracticeQuestionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPracticeSubmissionAnswerServiceImplTest {

    @Mock
    private UserPracticeSubmissionAnswerRepository answerRepository;

    @Mock
    private UserPracticeSubmissionRepository submissionRepository;

    @Mock
    private PracticeQuestionRepository practiceQuestionRepository;

    @Mock
    private SubmissionQuestionTypeAccuracyRepository submissionQuestionTypeAccuracyRepository;

    @Mock
    private SubmissionTopicTagAccuracyRepository submissionTopicTagAccuracyRepository;

    @Mock
    private IncludeSpec includes;

    @InjectMocks
    private UserPracticeSubmissionAnswerServiceImpl answerService;

    @Test
    void create_whenSubmissionExistsAndRequestIsValid_shouldSaveAnswerAndReturnAnswerId() {
        UserPracticeSubmission submission = submission("submission-1");
        UserPracticeSubmissionAnswerCreateRequestDto request = createRequest();

        when(submissionRepository.findById("submission-1")).thenReturn(Optional.of(submission));
        when(answerRepository.save(any(UserPracticeSubmissionAnswer.class))).thenAnswer(invocation -> {
            UserPracticeSubmissionAnswer answer = invocation.getArgument(0);
            answer.setId("answer-1");
            return answer;
        });

        UserPracticeSubmissionAnswerResponseDto result = answerService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("answer-1");

        ArgumentCaptor<UserPracticeSubmissionAnswer> answerCaptor =
                ArgumentCaptor.forClass(UserPracticeSubmissionAnswer.class);
        verify(answerRepository).save(answerCaptor.capture());

        UserPracticeSubmissionAnswer savedAnswer = answerCaptor.getValue();

        assertThat(savedAnswer.getSubmission()).isSameAs(submission);
        assertThat(savedAnswer.getOrderIndex()).isEqualTo(1);
        assertThat(savedAnswer.getAnswers()).containsExactly("A", "B");
        assertThat(savedAnswer.getAnswers()).isNotSameAs(request.getAnswers());

        verify(submissionRepository).findById("submission-1");
    }

    @Test
    void create_whenRequestAnswersAreEmpty_shouldSaveAnswerWithEmptyAnswers() {
        UserPracticeSubmission submission = submission("submission-1");
        UserPracticeSubmissionAnswerCreateRequestDto request = createRequest();
        request.setAnswers(List.of());

        when(submissionRepository.findById("submission-1")).thenReturn(Optional.of(submission));
        when(answerRepository.save(any(UserPracticeSubmissionAnswer.class))).thenAnswer(invocation -> {
            UserPracticeSubmissionAnswer answer = invocation.getArgument(0);
            answer.setId("answer-1");
            return answer;
        });

        UserPracticeSubmissionAnswerResponseDto result = answerService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("answer-1");

        ArgumentCaptor<UserPracticeSubmissionAnswer> answerCaptor =
                ArgumentCaptor.forClass(UserPracticeSubmissionAnswer.class);
        verify(answerRepository).save(answerCaptor.capture());

        assertThat(answerCaptor.getValue().getAnswers()).isEmpty();

        verify(submissionRepository).findById("submission-1");
    }

    @Test
    void create_whenRequestAnswersAreNull_shouldThrowNullPointerExceptionAndSkipSave() {
        UserPracticeSubmission submission = submission("submission-1");
        UserPracticeSubmissionAnswerCreateRequestDto request = createRequest();
        request.setAnswers(null);

        when(submissionRepository.findById("submission-1")).thenReturn(Optional.of(submission));

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> answerService.create(request)
        );

        assertThat(exception).isNotNull();

        verify(submissionRepository).findById("submission-1");
        verify(answerRepository, never()).save(any(UserPracticeSubmissionAnswer.class));
    }

    @Test
    void create_whenSubmissionDoesNotExist_shouldThrowRuntimeExceptionAndSkipSave() {
        UserPracticeSubmissionAnswerCreateRequestDto request = createRequest();

        when(submissionRepository.findById("submission-1")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> answerService.create(request)
        );

        assertThat(exception.getMessage()).isEqualTo("Submission not found");

        verify(submissionRepository).findById("submission-1");
        verify(answerRepository, never()).save(any(UserPracticeSubmissionAnswer.class));
    }

    @Test
    void createBulk_whenSubmissionExistsAndAnswersMatchQuestions_shouldSaveAnswersSetResultsUpdateSubmissionAndSaveAccuracies() {
        UserPracticeSubmission submission = submission("submission-1");
        UserPracticeSubmissionAnswerBulkCreateRequestDto request = bulkCreateRequest(
                answerRequest("submission-1", 1, List.of(" apple. ")),
                answerRequest("submission-1", 2, List.of("green", "blue")),
                answerRequest("submission-1", 3, List.of("London")),
                answerRequest("submission-1", 4, List.of("   "))
        );

        when(submissionRepository.findById("submission-1")).thenReturn(Optional.of(submission));
        when(answerRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<UserPracticeSubmissionAnswer> answers = invocation.getArgument(0);
            for (int index = 0; index < answers.size(); index++) {
                answers.get(index).setId("answer-" + (index + 1));
            }
            return answers;
        });
        when(practiceQuestionRepository.findByPracticeContent_IdOrderByOrderIndexAsc("content-1"))
                .thenReturn(List.of(
                        question(1, PracticeQuestionType.MULTIPLE_CHOICE,
                                PracticeTopicTag.EDUCATION_AND_LEARNING, List.of("Apple")),
                        question(2, PracticeQuestionType.MATCHING,
                                PracticeTopicTag.TECHNOLOGY_INTERNET_AND_AI, List.of("Blue", "Green")),
                        question(3, PracticeQuestionType.MULTIPLE_CHOICE,
                                PracticeTopicTag.TECHNOLOGY_INTERNET_AND_AI, List.of("Paris")),
                        question(4, PracticeQuestionType.SHORT_ANSWER_QUESTIONS,
                                PracticeTopicTag.EDUCATION_AND_LEARNING, List.of("Answer"))
                ));

        List<UserPracticeSubmissionAnswerResponseDto> result = answerService.createBulk(request);

        assertThat(result).hasSize(4);
        assertThat(result)
                .extracting(UserPracticeSubmissionAnswerResponseDto::getId)
                .containsExactly("answer-1", "answer-2", "answer-3", "answer-4");

        ArgumentCaptor<List<UserPracticeSubmissionAnswer>> answerListCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(answerRepository).saveAll(answerListCaptor.capture());

        List<UserPracticeSubmissionAnswer> savedAnswers = answerListCaptor.getValue();

        assertThat(savedAnswers).hasSize(4);
        assertThat(savedAnswers.get(0).getSubmission()).isSameAs(submission);
        assertThat(savedAnswers.get(0).getOrderIndex()).isEqualTo(1);
        assertThat(savedAnswers.get(0).getAnswers()).containsExactly(" apple. ");
        assertThat(savedAnswers.get(0).getResult()).isEqualTo(Result.CORRECT);
        assertThat(savedAnswers.get(1).getResult()).isEqualTo(Result.CORRECT);
        assertThat(savedAnswers.get(2).getResult()).isEqualTo(Result.WRONG);
        assertThat(savedAnswers.get(3).getResult()).isEqualTo(Result.SKIPPED);

        assertThat(submission.getCorrectAnswerCount()).isEqualTo(2);
        assertThat(submission.getWrongAnswerCount()).isEqualTo(1);
        assertThat(submission.getSkipAnswerCount()).isEqualTo(1);
        assertThat(submission.getTotalQuestionCount()).isEqualTo(4);
        assertThat(submission.getAnsweredQuestionCount()).isEqualTo(3);
        assertThat(submission.getAccuracyRate()).isEqualTo(50.0);
        assertThat(submission.getCorrectAnswerPercentage()).isEqualTo(50.0);
        assertThat(submission.getSkipRate()).isEqualTo(25.0);
        assertThat(submission.getEffectiveAccuracy()).isEqualTo(66.7);
        assertThat(submission.getScore()).isEqualTo(5.5);

        verify(submissionQuestionTypeAccuracyRepository).deleteBySubmission_Id("submission-1");
        verify(submissionTopicTagAccuracyRepository).deleteBySubmission_Id("submission-1");

        List<SubmissionQuestionTypeAccuracy> typeAccuracies = captureQuestionTypeAccuracies();
        assertThat(typeAccuracies).hasSize(3);

        SubmissionQuestionTypeAccuracy multipleChoiceAccuracy =
                findTypeAccuracy(typeAccuracies, PracticeQuestionType.MULTIPLE_CHOICE);
        assertThat(multipleChoiceAccuracy.getSubmission()).isSameAs(submission);
        assertThat(multipleChoiceAccuracy.getExposureCount()).isEqualTo(2);
        assertThat(multipleChoiceAccuracy.getAnsweredQuestionCount()).isEqualTo(2);
        assertThat(multipleChoiceAccuracy.getCorrectQuestionCount()).isEqualTo(1);
        assertThat(multipleChoiceAccuracy.getWrongQuestionCount()).isEqualTo(1);
        assertThat(multipleChoiceAccuracy.getSkipQuestionCount()).isEqualTo(0);
        assertThat(multipleChoiceAccuracy.getCorrectAnswerPercentage()).isEqualTo(50.0);
        assertThat(multipleChoiceAccuracy.getSkipRate()).isEqualTo(0.0);
        assertThat(multipleChoiceAccuracy.getEffectiveAccuracy()).isEqualTo(50.0);

        SubmissionQuestionTypeAccuracy matchingAccuracy =
                findTypeAccuracy(typeAccuracies, PracticeQuestionType.MATCHING);
        assertThat(matchingAccuracy.getExposureCount()).isEqualTo(1);
        assertThat(matchingAccuracy.getAnsweredQuestionCount()).isEqualTo(1);
        assertThat(matchingAccuracy.getCorrectQuestionCount()).isEqualTo(1);
        assertThat(matchingAccuracy.getCorrectAnswerPercentage()).isEqualTo(100.0);

        SubmissionQuestionTypeAccuracy shortAnswerAccuracy =
                findTypeAccuracy(typeAccuracies, PracticeQuestionType.SHORT_ANSWER_QUESTIONS);
        assertThat(shortAnswerAccuracy.getExposureCount()).isEqualTo(1);
        assertThat(shortAnswerAccuracy.getAnsweredQuestionCount()).isEqualTo(0);
        assertThat(shortAnswerAccuracy.getSkipQuestionCount()).isEqualTo(1);
        assertThat(shortAnswerAccuracy.getSkipRate()).isEqualTo(100.0);
        assertThat(shortAnswerAccuracy.getEffectiveAccuracy()).isEqualTo(0.0);

        List<SubmissionTopicTagAccuracy> topicTagAccuracies = captureTopicTagAccuracies();
        assertThat(topicTagAccuracies).hasSize(2);

        SubmissionTopicTagAccuracy educationAccuracy =
                findTopicTagAccuracy(topicTagAccuracies, PracticeTopicTag.EDUCATION_AND_LEARNING);
        assertThat(educationAccuracy.getSubmission()).isSameAs(submission);
        assertThat(educationAccuracy.getExposureCount()).isEqualTo(2);
        assertThat(educationAccuracy.getAnsweredQuestionCount()).isEqualTo(1);
        assertThat(educationAccuracy.getCorrectQuestionCount()).isEqualTo(1);
        assertThat(educationAccuracy.getSkipQuestionCount()).isEqualTo(1);
        assertThat(educationAccuracy.getCorrectAnswerPercentage()).isEqualTo(50.0);
        assertThat(educationAccuracy.getSkipRate()).isEqualTo(50.0);
        assertThat(educationAccuracy.getEffectiveAccuracy()).isEqualTo(100.0);

        SubmissionTopicTagAccuracy technologyAccuracy =
                findTopicTagAccuracy(topicTagAccuracies, PracticeTopicTag.TECHNOLOGY_INTERNET_AND_AI);
        assertThat(technologyAccuracy.getExposureCount()).isEqualTo(2);
        assertThat(technologyAccuracy.getAnsweredQuestionCount()).isEqualTo(2);
        assertThat(technologyAccuracy.getCorrectQuestionCount()).isEqualTo(1);
        assertThat(technologyAccuracy.getWrongQuestionCount()).isEqualTo(1);
        assertThat(technologyAccuracy.getCorrectAnswerPercentage()).isEqualTo(50.0);
        assertThat(technologyAccuracy.getSkipRate()).isEqualTo(0.0);
        assertThat(technologyAccuracy.getEffectiveAccuracy()).isEqualTo(50.0);

        verify(submissionRepository).findById("submission-1");
        verify(practiceQuestionRepository).findByPracticeContent_IdOrderByOrderIndexAsc("content-1");
    }

    @Test
    void createBulk_whenSingleAnswerMatchesOneOfMultipleCorrectAnswers_shouldMarkAnswerCorrect() {
        UserPracticeSubmission submission = submission("submission-1");
        UserPracticeSubmissionAnswerBulkCreateRequestDto request = bulkCreateRequest(
                answerRequest("submission-1", 1, List.of("second option"))
        );

        when(submissionRepository.findById("submission-1")).thenReturn(Optional.of(submission));
        when(answerRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<UserPracticeSubmissionAnswer> answers = invocation.getArgument(0);
            answers.get(0).setId("answer-1");
            return answers;
        });
        when(practiceQuestionRepository.findByPracticeContent_IdOrderByOrderIndexAsc("content-1"))
                .thenReturn(List.of(
                        question(1, PracticeQuestionType.MULTIPLE_CHOICE,
                                PracticeTopicTag.EDUCATION_AND_LEARNING, List.of("First option", "Second option"))
                ));

        List<UserPracticeSubmissionAnswerResponseDto> result = answerService.createBulk(request);

        assertThat(result).hasSize(1);

        ArgumentCaptor<List<UserPracticeSubmissionAnswer>> answerListCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(answerRepository).saveAll(answerListCaptor.capture());

        assertThat(answerListCaptor.getValue().get(0).getResult()).isEqualTo(Result.CORRECT);
        assertThat(submission.getCorrectAnswerCount()).isEqualTo(1);
        assertThat(submission.getWrongAnswerCount()).isZero();
        assertThat(submission.getSkipAnswerCount()).isZero();
        assertThat(submission.getScore()).isEqualTo(9.0);
    }

    @Test
    void createBulk_whenMultiAnswerHasExtraWrongOption_shouldMarkAnswerWrong() {
        UserPracticeSubmission submission = submission("submission-1");
        UserPracticeSubmissionAnswerBulkCreateRequestDto request = bulkCreateRequest(
                answerRequest("submission-1", 1, List.of("green", "red"))
        );

        when(submissionRepository.findById("submission-1")).thenReturn(Optional.of(submission));
        when(answerRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<UserPracticeSubmissionAnswer> answers = invocation.getArgument(0);
            answers.get(0).setId("answer-1");
            return answers;
        });
        when(practiceQuestionRepository.findByPracticeContent_IdOrderByOrderIndexAsc("content-1"))
                .thenReturn(List.of(
                        question(1, PracticeQuestionType.MATCHING,
                                PracticeTopicTag.TECHNOLOGY_INTERNET_AND_AI, List.of("green", "blue"))
                ));

        answerService.createBulk(request);

        ArgumentCaptor<List<UserPracticeSubmissionAnswer>> answerListCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(answerRepository).saveAll(answerListCaptor.capture());

        assertThat(answerListCaptor.getValue().get(0).getResult()).isEqualTo(Result.WRONG);
        assertThat(submission.getCorrectAnswerCount()).isZero();
        assertThat(submission.getWrongAnswerCount()).isEqualTo(1);
        assertThat(submission.getScore()).isEqualTo(0.0);
    }

    @Test
    void createBulk_whenQuestionHasNoCorrectAnswers_shouldMarkAnsweredQuestionWrong() {
        UserPracticeSubmission submission = submission("submission-1");
        UserPracticeSubmissionAnswerBulkCreateRequestDto request = bulkCreateRequest(
                answerRequest("submission-1", 1, List.of("A"))
        );

        when(submissionRepository.findById("submission-1")).thenReturn(Optional.of(submission));
        when(answerRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<UserPracticeSubmissionAnswer> answers = invocation.getArgument(0);
            answers.get(0).setId("answer-1");
            return answers;
        });
        when(practiceQuestionRepository.findByPracticeContent_IdOrderByOrderIndexAsc("content-1"))
                .thenReturn(List.of(
                        question(1, PracticeQuestionType.MULTIPLE_CHOICE,
                                PracticeTopicTag.EDUCATION_AND_LEARNING, List.of())
                ));

        answerService.createBulk(request);

        ArgumentCaptor<List<UserPracticeSubmissionAnswer>> answerListCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(answerRepository).saveAll(answerListCaptor.capture());

        assertThat(answerListCaptor.getValue().get(0).getResult()).isEqualTo(Result.WRONG);
        assertThat(submission.getCorrectAnswerCount()).isZero();
        assertThat(submission.getWrongAnswerCount()).isEqualTo(1);
        assertThat(submission.getSkipAnswerCount()).isZero();
        assertThat(submission.getEffectiveAccuracy()).isEqualTo(0.0);
        assertThat(submission.getScore()).isEqualTo(0.0);
    }

    @Test
    void createBulk_whenQuestionCorrectAnswersAreNull_shouldMarkAnsweredQuestionWrong() {
        UserPracticeSubmission submission = submission("submission-1");
        UserPracticeSubmissionAnswerBulkCreateRequestDto request = bulkCreateRequest(
                answerRequest("submission-1", 1, List.of("A"))
        );

        when(submissionRepository.findById("submission-1")).thenReturn(Optional.of(submission));
        when(answerRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<UserPracticeSubmissionAnswer> answers = invocation.getArgument(0);
            answers.get(0).setId("answer-1");
            return answers;
        });
        when(practiceQuestionRepository.findByPracticeContent_IdOrderByOrderIndexAsc("content-1"))
                .thenReturn(List.of(
                        question(1, PracticeQuestionType.MULTIPLE_CHOICE,
                                PracticeTopicTag.EDUCATION_AND_LEARNING, null)
                ));

        answerService.createBulk(request);

        ArgumentCaptor<List<UserPracticeSubmissionAnswer>> answerListCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(answerRepository).saveAll(answerListCaptor.capture());

        assertThat(answerListCaptor.getValue().get(0).getResult()).isEqualTo(Result.WRONG);
        assertThat(submission.getWrongAnswerCount()).isEqualTo(1);
        assertThat(submission.getScore()).isEqualTo(0.0);
    }

    @Test
    void createBulk_whenUserAnswerNormalizesToBlank_shouldMarkAnsweredQuestionWrong() {
        UserPracticeSubmission submission = submission("submission-1");
        UserPracticeSubmissionAnswerBulkCreateRequestDto request = bulkCreateRequest(
                answerRequest("submission-1", 1, List.of("!!!"))
        );

        when(submissionRepository.findById("submission-1")).thenReturn(Optional.of(submission));
        when(answerRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<UserPracticeSubmissionAnswer> answers = invocation.getArgument(0);
            answers.get(0).setId("answer-1");
            return answers;
        });
        when(practiceQuestionRepository.findByPracticeContent_IdOrderByOrderIndexAsc("content-1"))
                .thenReturn(List.of(
                        question(1, PracticeQuestionType.MULTIPLE_CHOICE,
                                PracticeTopicTag.EDUCATION_AND_LEARNING, List.of("A"))
                ));

        answerService.createBulk(request);

        ArgumentCaptor<List<UserPracticeSubmissionAnswer>> answerListCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(answerRepository).saveAll(answerListCaptor.capture());

        assertThat(answerListCaptor.getValue().get(0).getResult()).isEqualTo(Result.WRONG);
        assertThat(submission.getWrongAnswerCount()).isEqualTo(1);
    }

    @Test
    void createBulk_whenAnswerListContainsOnlyNull_shouldMarkAnswerSkipped() {
        UserPracticeSubmission submission = submission("submission-1");
        UserPracticeSubmissionAnswerBulkCreateRequestDto request = bulkCreateRequest(
                answerRequest("submission-1", 1, Arrays.asList((String) null))
        );

        when(submissionRepository.findById("submission-1")).thenReturn(Optional.of(submission));
        when(answerRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<UserPracticeSubmissionAnswer> answers = invocation.getArgument(0);
            answers.get(0).setId("answer-1");
            return answers;
        });
        when(practiceQuestionRepository.findByPracticeContent_IdOrderByOrderIndexAsc("content-1"))
                .thenReturn(List.of(
                        question(1, PracticeQuestionType.MULTIPLE_CHOICE,
                                PracticeTopicTag.EDUCATION_AND_LEARNING, List.of("A"))
                ));

        answerService.createBulk(request);

        ArgumentCaptor<List<UserPracticeSubmissionAnswer>> answerListCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(answerRepository).saveAll(answerListCaptor.capture());

        assertThat(answerListCaptor.getValue().get(0).getResult()).isEqualTo(Result.SKIPPED);
        assertThat(submission.getSkipAnswerCount()).isEqualTo(1);
        assertThat(submission.getAnsweredQuestionCount()).isZero();
    }

    @Test
    void createBulk_whenAnswerListIsEmpty_shouldMarkAnswerSkipped() {
        UserPracticeSubmission submission = submission("submission-1");
        UserPracticeSubmissionAnswerBulkCreateRequestDto request = bulkCreateRequest(
                answerRequest("submission-1", 1, List.of())
        );

        when(submissionRepository.findById("submission-1")).thenReturn(Optional.of(submission));
        when(answerRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<UserPracticeSubmissionAnswer> answers = invocation.getArgument(0);
            answers.get(0).setId("answer-1");
            return answers;
        });
        when(practiceQuestionRepository.findByPracticeContent_IdOrderByOrderIndexAsc("content-1"))
                .thenReturn(List.of(
                        question(1, PracticeQuestionType.MULTIPLE_CHOICE,
                                PracticeTopicTag.EDUCATION_AND_LEARNING, List.of("A"))
                ));

        answerService.createBulk(request);

        ArgumentCaptor<List<UserPracticeSubmissionAnswer>> answerListCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(answerRepository).saveAll(answerListCaptor.capture());

        assertThat(answerListCaptor.getValue().get(0).getResult()).isEqualTo(Result.SKIPPED);
        assertThat(submission.getSkipAnswerCount()).isEqualTo(1);
    }

    @Test
    void createBulk_whenSavedAnswerIsMissingForQuestion_shouldCountQuestionAsSkippedWithoutChangingSavedAnswer() {
        UserPracticeSubmission submission = submission("submission-1");
        UserPracticeSubmissionAnswerBulkCreateRequestDto request = bulkCreateRequest(
                answerRequest("submission-1", 1, List.of("A"))
        );

        when(submissionRepository.findById("submission-1")).thenReturn(Optional.of(submission));
        when(answerRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<UserPracticeSubmissionAnswer> answers = invocation.getArgument(0);
            answers.get(0).setId("answer-1");
            return answers;
        });
        when(practiceQuestionRepository.findByPracticeContent_IdOrderByOrderIndexAsc("content-1"))
                .thenReturn(List.of(
                        question(1, PracticeQuestionType.MULTIPLE_CHOICE,
                                PracticeTopicTag.EDUCATION_AND_LEARNING, List.of("A")),
                        question(2, PracticeQuestionType.MATCHING,
                                PracticeTopicTag.TECHNOLOGY_INTERNET_AND_AI, List.of("B"))
                ));

        answerService.createBulk(request);

        ArgumentCaptor<List<UserPracticeSubmissionAnswer>> answerListCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(answerRepository).saveAll(answerListCaptor.capture());

        assertThat(answerListCaptor.getValue()).hasSize(1);
        assertThat(answerListCaptor.getValue().get(0).getResult()).isEqualTo(Result.CORRECT);

        assertThat(submission.getCorrectAnswerCount()).isEqualTo(1);
        assertThat(submission.getWrongAnswerCount()).isZero();
        assertThat(submission.getSkipAnswerCount()).isEqualTo(1);
        assertThat(submission.getTotalQuestionCount()).isEqualTo(2);
        assertThat(submission.getAnsweredQuestionCount()).isEqualTo(1);
        assertThat(submission.getAccuracyRate()).isEqualTo(50.0);
        assertThat(submission.getSkipRate()).isEqualTo(50.0);
        assertThat(submission.getEffectiveAccuracy()).isEqualTo(100.0);
    }

    @Test
    void createBulk_whenQuestionsDoNotExist_shouldSetZeroMetricsDeleteOldAccuraciesAndSaveEmptyAccuracies() {
        UserPracticeSubmission submission = submission("submission-1");
        UserPracticeSubmissionAnswerBulkCreateRequestDto request = bulkCreateRequest(
                answerRequest("submission-1", 1, List.of("A"))
        );

        when(submissionRepository.findById("submission-1")).thenReturn(Optional.of(submission));
        when(answerRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<UserPracticeSubmissionAnswer> answers = invocation.getArgument(0);
            answers.get(0).setId("answer-1");
            return answers;
        });
        when(practiceQuestionRepository.findByPracticeContent_IdOrderByOrderIndexAsc("content-1"))
                .thenReturn(List.of());

        List<UserPracticeSubmissionAnswerResponseDto> result = answerService.createBulk(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("answer-1");

        assertThat(submission.getCorrectAnswerCount()).isEqualTo(0);
        assertThat(submission.getWrongAnswerCount()).isEqualTo(0);
        assertThat(submission.getSkipAnswerCount()).isEqualTo(0);
        assertThat(submission.getTotalQuestionCount()).isEqualTo(0);
        assertThat(submission.getAnsweredQuestionCount()).isEqualTo(0);
        assertThat(submission.getAccuracyRate()).isEqualTo(0.0);
        assertThat(submission.getCorrectAnswerPercentage()).isEqualTo(0.0);
        assertThat(submission.getSkipRate()).isEqualTo(0.0);
        assertThat(submission.getEffectiveAccuracy()).isEqualTo(0.0);
        assertThat(submission.getScore()).isEqualTo(0.0);

        verify(submissionQuestionTypeAccuracyRepository).deleteBySubmission_Id("submission-1");
        verify(submissionTopicTagAccuracyRepository).deleteBySubmission_Id("submission-1");
        assertThat(captureQuestionTypeAccuracies()).isEmpty();
        assertThat(captureTopicTagAccuracies()).isEmpty();
    }

    @Test
    void createBulk_whenRequestAnswersAreEmpty_shouldSaveNoAnswersAndMarkAllQuestionsSkippedInSubmissionMetrics() {
        UserPracticeSubmission submission = submission("submission-1");
        UserPracticeSubmissionAnswerBulkCreateRequestDto request = bulkCreateRequest();

        when(submissionRepository.findById("submission-1")).thenReturn(Optional.of(submission));
        when(answerRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(practiceQuestionRepository.findByPracticeContent_IdOrderByOrderIndexAsc("content-1"))
                .thenReturn(List.of(
                        question(1, PracticeQuestionType.MULTIPLE_CHOICE,
                                PracticeTopicTag.EDUCATION_AND_LEARNING, List.of("A")),
                        question(2, PracticeQuestionType.MATCHING,
                                PracticeTopicTag.TECHNOLOGY_INTERNET_AND_AI, List.of("B"))
                ));

        List<UserPracticeSubmissionAnswerResponseDto> result = answerService.createBulk(request);

        assertThat(result).isEmpty();

        assertThat(submission.getCorrectAnswerCount()).isEqualTo(0);
        assertThat(submission.getWrongAnswerCount()).isEqualTo(0);
        assertThat(submission.getSkipAnswerCount()).isEqualTo(2);
        assertThat(submission.getTotalQuestionCount()).isEqualTo(2);
        assertThat(submission.getAnsweredQuestionCount()).isEqualTo(0);
        assertThat(submission.getAccuracyRate()).isEqualTo(0.0);
        assertThat(submission.getSkipRate()).isEqualTo(100.0);
        assertThat(submission.getEffectiveAccuracy()).isEqualTo(0.0);
        assertThat(submission.getScore()).isEqualTo(0.0);

        List<SubmissionQuestionTypeAccuracy> typeAccuracies = captureQuestionTypeAccuracies();
        assertThat(typeAccuracies).hasSize(2);
        assertThat(typeAccuracies)
                .extracting(SubmissionQuestionTypeAccuracy::getSkipQuestionCount)
                .containsExactlyInAnyOrder(1, 1);
    }

    @Test
    void createBulk_whenRequestAnswersIsNull_shouldThrowNullPointerExceptionAndSkipSaveAll() {
        UserPracticeSubmission submission = submission("submission-1");
        UserPracticeSubmissionAnswerBulkCreateRequestDto request =
                new UserPracticeSubmissionAnswerBulkCreateRequestDto();
        request.setUserPracticeSubmissionId("submission-1");
        request.setAnswers(null);

        when(submissionRepository.findById("submission-1")).thenReturn(Optional.of(submission));

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> answerService.createBulk(request)
        );

        assertThat(exception).isNotNull();

        verify(submissionRepository).findById("submission-1");
        verify(answerRepository, never()).saveAll(anyList());
        verify(practiceQuestionRepository, never()).findByPracticeContent_IdOrderByOrderIndexAsc(anyString());
    }

    @Test
    void createBulk_whenAnswerRequestContainsNullAnswers_shouldThrowNullPointerExceptionBeforeScoring() {
        UserPracticeSubmission submission = submission("submission-1");
        UserPracticeSubmissionAnswerBulkCreateRequestDto request = bulkCreateRequest(
                answerRequest("submission-1", 1, null)
        );

        when(submissionRepository.findById("submission-1")).thenReturn(Optional.of(submission));

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> answerService.createBulk(request)
        );

        assertThat(exception).isNotNull();

        verify(answerRepository, never()).saveAll(anyList());
        verify(practiceQuestionRepository, never()).findByPracticeContent_IdOrderByOrderIndexAsc(anyString());
    }

    @Test
    void createBulk_whenSubmissionDoesNotExist_shouldThrowRuntimeExceptionAndSkipSave() {
        UserPracticeSubmissionAnswerBulkCreateRequestDto request = bulkCreateRequest(
                answerRequest("missing-submission", 1, List.of("A"))
        );
        request.setUserPracticeSubmissionId("missing-submission");

        when(submissionRepository.findById("missing-submission")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> answerService.createBulk(request)
        );

        assertThat(exception.getMessage()).isEqualTo("Submission not found");

        verify(submissionRepository).findById("missing-submission");
        verify(answerRepository, never()).saveAll(anyList());
        verify(practiceQuestionRepository, never()).findByPracticeContent_IdOrderByOrderIndexAsc(anyString());
    }

    @ParameterizedTest
    @CsvSource({
            "39, 9.0",
            "37, 8.5",
            "35, 8.0",
            "32, 7.5",
            "30, 7.0",
            "26, 6.5",
            "23, 6.0",
            "18, 5.5",
            "16, 5.0",
            "13, 4.5",
            "11, 4.0",
            "8, 3.5",
            "6, 3.0",
            "4, 2.5",
            "2, 2.0",
            "1, 1.5",
            "0, 0.0"
    })
    void createBulk_whenCorrectAnswerPercentageMatchesBandThreshold_shouldSetExpectedBandScore(
            int correctCount,
            double expectedScore
    ) {
        UserPracticeSubmission submission = submission("submission-1");
        UserPracticeSubmissionAnswerBulkCreateRequestDto request =
                bulkCreateRequestForBandThreshold(correctCount, 40);

        when(submissionRepository.findById("submission-1")).thenReturn(Optional.of(submission));
        when(answerRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<UserPracticeSubmissionAnswer> answers = invocation.getArgument(0);
            for (int index = 0; index < answers.size(); index++) {
                answers.get(index).setId("answer-" + (index + 1));
            }
            return answers;
        });
        when(practiceQuestionRepository.findByPracticeContent_IdOrderByOrderIndexAsc("content-1"))
                .thenReturn(questionsForBandThreshold(40));

        answerService.createBulk(request);

        assertThat(submission.getCorrectAnswerCount()).isEqualTo(correctCount);
        assertThat(submission.getWrongAnswerCount()).isEqualTo(40 - correctCount);
        assertThat(submission.getSkipAnswerCount()).isZero();
        assertThat(submission.getTotalQuestionCount()).isEqualTo(40);
        assertThat(submission.getAnsweredQuestionCount()).isEqualTo(40);
        assertThat(submission.getScore()).isEqualTo(expectedScore);

        verify(submissionQuestionTypeAccuracyRepository).deleteBySubmission_Id("submission-1");
        verify(submissionTopicTagAccuracyRepository).deleteBySubmission_Id("submission-1");
    }

    @ParameterizedTest
    @CsvSource({
            "39, 9.0",
            "37, 8.5",
            "35, 8.0",
            "32, 7.5",
            "30, 7.0",
            "26, 6.5",
            "23, 6.0",
            "18, 5.5",
            "16, 5.0",
            "13, 4.5",
            "11, 4.0",
            "8, 3.5",
            "6, 3.0",
            "4, 2.5",
            "2, 2.0",
            "1, 1.5",
            "0, 0.0"
    })
    void bandScoreFromCorrect_whenCorrectCountMatchesThreshold_shouldReturnExpectedBandScore(
            int correctCount,
            double expectedScore
    ) {
        Double result = invokePrivateMethod("bandScoreFromCorrect", new Class<?>[]{int.class}, correctCount);

        assertThat(result).isEqualTo(expectedScore);
    }

    @ParameterizedTest
    @CsvSource({
            "97.5, 9.0",
            "92.5, 8.5",
            "87.5, 8.0",
            "80.0, 7.5",
            "75.0, 7.0",
            "65.0, 6.5",
            "57.5, 6.0",
            "45.0, 5.5",
            "40.0, 5.0",
            "32.5, 4.5",
            "27.5, 4.0",
            "20.0, 3.5",
            "15.0, 3.0",
            "10.0, 2.5",
            "5.0, 2.0",
            "2.5, 1.5",
            "0.0, 0.0"
    })
    void bandScoreFromPercentage_whenPercentageMatchesThreshold_shouldReturnExpectedBandScore(
            double percentage,
            double expectedScore
    ) {
        Double result = invokePrivateMethod("bandScoreFromPercentage", new Class<?>[]{double.class}, percentage);

        assertThat(result).isEqualTo(expectedScore);
    }

    @ParameterizedTest
    @CsvSource({
            "66.66, 66.7",
            "66.64, 66.6",
            "0.04, 0.0",
            "0.05, 0.1"
    })
    void roundToOneDecimal_whenValueHasMultipleDecimals_shouldRoundToOneDecimal(
            double value,
            double expected
    ) {
        Double result = invokePrivateMethod("roundToOneDecimal", new Class<?>[]{double.class}, value);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void isAnswerEmpty_whenAnswersAreNullEmptyBlankOrNullValues_shouldReturnTrue() {
        assertThat((Boolean) invokePrivateMethod("isAnswerEmpty", new Class<?>[]{List.class}, new Object[]{null}))
                .isTrue();
        assertThat((Boolean) invokePrivateMethod("isAnswerEmpty", new Class<?>[]{List.class}, List.of()))
                .isTrue();
        assertThat((Boolean) invokePrivateMethod("isAnswerEmpty", new Class<?>[]{List.class}, List.of("   ")))
                .isTrue();
        assertThat((Boolean) invokePrivateMethod("isAnswerEmpty", new Class<?>[]{List.class}, Arrays.asList((String) null)))
                .isTrue();
    }

    @Test
    void isAnswerEmpty_whenAtLeastOneAnswerHasText_shouldReturnFalse() {
        assertThat((Boolean) invokePrivateMethod("isAnswerEmpty", new Class<?>[]{List.class}, List.of(" A ")))
                .isFalse();
        assertThat((Boolean) invokePrivateMethod("isAnswerEmpty", new Class<?>[]{List.class}, Arrays.asList(null, " B ")))
                .isFalse();
    }

    @Test
    void normalizeToSet_whenValuesAreNullOrBlankAfterNormalization_shouldReturnExpectedSet() {
        Set<String> nullResult = invokePrivateMethod("normalizeToSet", new Class<?>[]{List.class}, new Object[]{null});
        Set<String> blankResult = invokePrivateMethod("normalizeToSet", new Class<?>[]{List.class}, Arrays.asList(null, "!!!", "  "));
        Set<String> textResult = invokePrivateMethod("normalizeToSet", new Class<?>[]{List.class}, List.of("  Hello   World!!! ", "HELLO WORLD"));

        assertThat(nullResult).isEmpty();
        assertThat(blankResult).isEmpty();
        assertThat(textResult).containsExactly("hello world");
    }

    @Test
    void normalize_whenInputIsNullOrContainsExtraSpacesAndTrailingPunctuation_shouldReturnNormalizedValue() {
        assertThat((String) invokePrivateMethod("normalize", new Class<?>[]{String.class}, new Object[]{null}))
                .isEmpty();
        assertThat((String) invokePrivateMethod("normalize", new Class<?>[]{String.class}, "  Hello    World!!! "))
                .isEqualTo("hello world");
    }

    @Test
    void isCorrect_whenAnswerSetsCoverAllBranches_shouldReturnExpectedResult() {
        assertThat((Boolean) invokePrivateMethod(
                "isCorrect",
                new Class<?>[]{List.class, List.class},
                List.of("A"),
                List.of("A", "B")
        )).isTrue();

        assertThat((Boolean) invokePrivateMethod(
                "isCorrect",
                new Class<?>[]{List.class, List.class},
                List.of("A", "B"),
                List.of("B", "A")
        )).isTrue();

        assertThat((Boolean) invokePrivateMethod(
                "isCorrect",
                new Class<?>[]{List.class, List.class},
                List.of("A", "C"),
                List.of("A", "B")
        )).isFalse();

        assertThat((Boolean) invokePrivateMethod(
                "isCorrect",
                new Class<?>[]{List.class, List.class},
                List.of("!!!"),
                List.of("A")
        )).isFalse();

        assertThat((Boolean) invokePrivateMethod(
                "isCorrect",
                new Class<?>[]{List.class, List.class},
                List.of("A"),
                null
        )).isFalse();
    }

    @Test
    void getAllBySubmissionId_whenAnswersExistAndFieldsAreIncluded_shouldReturnDtosWithIncludedFields() {
        String submissionId = "submission-1";
        UserPracticeSubmission submission = submission(submissionId);
        UserPracticeSubmissionAnswer answer1 = answer("answer-1", submission, 1, List.of("A"), Result.CORRECT);
        UserPracticeSubmissionAnswer answer2 = answer("answer-2", submission, 2, List.of("B"), Result.WRONG);

        when(answerRepository.findAllBySubmission_IdOrderByOrderIndexAsc(submissionId))
                .thenReturn(List.of(answer1, answer2));
        mockIncludes("submissionid", "orderindex", "answers", "result");

        List<UserPracticeSubmissionAnswerResponseDto> result =
                answerService.getAllBySubmissionId(submissionId, includes);

        assertThat(result).hasSize(2);

        assertThat(result.get(0).getId()).isEqualTo("answer-1");
        assertThat(result.get(0).getSubmissionId()).isEqualTo(submissionId);
        assertThat(result.get(0).getOrderIndex()).isEqualTo(1);
        assertThat(result.get(0).getAnswers()).containsExactly("A");
        assertThat(result.get(0).getAnswers()).isNotSameAs(answer1.getAnswers());
        assertThat(result.get(0).getResult()).isEqualTo(Result.CORRECT);

        assertThat(result.get(1).getId()).isEqualTo("answer-2");
        assertThat(result.get(1).getSubmissionId()).isEqualTo(submissionId);
        assertThat(result.get(1).getOrderIndex()).isEqualTo(2);
        assertThat(result.get(1).getAnswers()).containsExactly("B");
        assertThat(result.get(1).getResult()).isEqualTo(Result.WRONG);

        verify(answerRepository).findAllBySubmission_IdOrderByOrderIndexAsc(submissionId);
    }

    @Test
    void getAllBySubmissionId_whenNoFieldsAreIncluded_shouldReturnOnlyIds() {
        String submissionId = "submission-1";
        UserPracticeSubmission submission = submission(submissionId);
        UserPracticeSubmissionAnswer answer = answer("answer-1", submission, 1, List.of("A"), Result.CORRECT);

        when(answerRepository.findAllBySubmission_IdOrderByOrderIndexAsc(submissionId))
                .thenReturn(List.of(answer));
        mockIncludes();

        List<UserPracticeSubmissionAnswerResponseDto> result =
                answerService.getAllBySubmissionId(submissionId, includes);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("answer-1");
        assertThat(result.get(0).getSubmissionId()).isNull();
        assertThat(result.get(0).getOrderIndex()).isNull();
        assertThat(result.get(0).getAnswers()).isNull();
        assertThat(result.get(0).getResult()).isNull();

        verify(answerRepository).findAllBySubmission_IdOrderByOrderIndexAsc(submissionId);
    }

    @Test
    void getAllBySubmissionId_whenRepositoryReturnsEmptyList_shouldReturnEmptyList() {
        String submissionId = "submission-1";

        when(answerRepository.findAllBySubmission_IdOrderByOrderIndexAsc(submissionId))
                .thenReturn(List.of());

        List<UserPracticeSubmissionAnswerResponseDto> result =
                answerService.getAllBySubmissionId(submissionId, includes);

        assertThat(result).isEmpty();

        verify(answerRepository).findAllBySubmission_IdOrderByOrderIndexAsc(submissionId);
    }

    @Test
    void getById_whenAnswerExistsAndFieldsAreIncluded_shouldReturnDtoWithIncludedFields() {
        UserPracticeSubmission submission = submission("submission-1");
        UserPracticeSubmissionAnswer answer =
                answer("answer-1", submission, 1, List.of("A"), Result.CORRECT);

        when(answerRepository.findById("answer-1")).thenReturn(Optional.of(answer));
        mockIncludes("submissionid", "orderindex", "answers", "result");

        UserPracticeSubmissionAnswerResponseDto result = answerService.getById("answer-1", includes);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("answer-1");
        assertThat(result.getSubmissionId()).isEqualTo("submission-1");
        assertThat(result.getOrderIndex()).isEqualTo(1);
        assertThat(result.getAnswers()).containsExactly("A");
        assertThat(result.getResult()).isEqualTo(Result.CORRECT);

        verify(answerRepository).findById("answer-1");
    }

    @Test
    void getById_whenNoFieldsAreIncluded_shouldReturnOnlyAnswerId() {
        UserPracticeSubmission submission = submission("submission-1");
        UserPracticeSubmissionAnswer answer =
                answer("answer-1", submission, 1, List.of("A"), Result.CORRECT);

        when(answerRepository.findById("answer-1")).thenReturn(Optional.of(answer));
        mockIncludes();

        UserPracticeSubmissionAnswerResponseDto result = answerService.getById("answer-1", includes);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("answer-1");
        assertThat(result.getSubmissionId()).isNull();
        assertThat(result.getOrderIndex()).isNull();
        assertThat(result.getAnswers()).isNull();
        assertThat(result.getResult()).isNull();

        verify(answerRepository).findById("answer-1");
    }

    @Test
    void getById_whenAnswerDoesNotExist_shouldThrowRuntimeException() {
        when(answerRepository.findById("missing-answer")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> answerService.getById("missing-answer", includes)
        );

        assertThat(exception.getMessage()).isEqualTo("Submission answer not found");

        verify(answerRepository).findById("missing-answer");
    }

    private UserPracticeSubmissionAnswerCreateRequestDto createRequest() {
        return answerRequest("submission-1", 1, List.of("A", "B"));
    }

    private UserPracticeSubmissionAnswerCreateRequestDto answerRequest(String submissionId,
                                                                       Integer orderIndex,
                                                                       List<String> answers) {
        UserPracticeSubmissionAnswerCreateRequestDto request = new UserPracticeSubmissionAnswerCreateRequestDto();

        request.setUserPracticeSubmissionId(submissionId);
        request.setOrderIndex(orderIndex);
        request.setAnswers(answers);

        return request;
    }

    private UserPracticeSubmissionAnswerBulkCreateRequestDto bulkCreateRequest(
            UserPracticeSubmissionAnswerCreateRequestDto... answers
    ) {
        UserPracticeSubmissionAnswerBulkCreateRequestDto request =
                new UserPracticeSubmissionAnswerBulkCreateRequestDto();

        request.setUserPracticeSubmissionId("submission-1");
        request.setAnswers(new ArrayList<>(Arrays.asList(answers)));

        return request;
    }

    private UserPracticeSubmissionAnswerBulkCreateRequestDto bulkCreateRequestForBandThreshold(
            int correctCount,
            int totalQuestions
    ) {
        List<UserPracticeSubmissionAnswerCreateRequestDto> answers = new ArrayList<>();

        for (int orderIndex = 1; orderIndex <= totalQuestions; orderIndex++) {
            List<String> answerValues = orderIndex <= correctCount
                    ? List.of("A")
                    : List.of("wrong");

            answers.add(answerRequest("submission-1", orderIndex, answerValues));
        }

        UserPracticeSubmissionAnswerBulkCreateRequestDto request =
                new UserPracticeSubmissionAnswerBulkCreateRequestDto();

        request.setUserPracticeSubmissionId("submission-1");
        request.setAnswers(answers);

        return request;
    }

    private List<PracticeQuestion> questionsForBandThreshold(int totalQuestions) {
        List<PracticeQuestion> questions = new ArrayList<>();

        for (int orderIndex = 1; orderIndex <= totalQuestions; orderIndex++) {
            questions.add(question(
                    orderIndex,
                    PracticeQuestionType.MULTIPLE_CHOICE,
                    PracticeTopicTag.EDUCATION_AND_LEARNING,
                    List.of("A")
            ));
        }

        return questions;
    }

    private UserPracticeSubmission submission(String id) {
        UserPracticeSubmission submission = new UserPracticeSubmission();

        submission.setId(id);
        submission.setPracticeContentId("content-1");

        return submission;
    }

    private UserPracticeSubmissionAnswer answer(String id,
                                                UserPracticeSubmission submission,
                                                Integer orderIndex,
                                                List<String> answers,
                                                Result result) {
        UserPracticeSubmissionAnswer answer = new UserPracticeSubmissionAnswer();

        answer.setId(id);
        answer.setSubmission(submission);
        answer.setOrderIndex(orderIndex);
        answer.setAnswers(new ArrayList<>(answers));
        answer.setResult(result);

        return answer;
    }

    private PracticeQuestion question(Integer orderIndex,
                                      PracticeQuestionType type,
                                      PracticeTopicTag topicTag,
                                      List<String> correctAnswers) {
        PracticeQuestion question = new PracticeQuestion();

        setIfPresent(question, "setId", "question-" + orderIndex);
        question.setOrderIndex(orderIndex);
        question.setType(type);
        question.setTopicTag(topicTag);
        question.setCorrectAnswers(correctAnswers);

        return question;
    }

    private void mockIncludes(String... fieldsToInclude) {
        Set<String> includedFields = Set.copyOf(Arrays.asList(fieldsToInclude));

        when(includes.has(anyString())).thenAnswer(invocation -> {
            String fieldName = invocation.getArgument(0);
            return includedFields.contains(fieldName);
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<SubmissionQuestionTypeAccuracy> captureQuestionTypeAccuracies() {
        ArgumentCaptor<Iterable<SubmissionQuestionTypeAccuracy>> captor =
                ArgumentCaptor.forClass((Class) Iterable.class);

        verify(submissionQuestionTypeAccuracyRepository).saveAll(captor.capture());

        List<SubmissionQuestionTypeAccuracy> result = new ArrayList<>();
        captor.getValue().forEach(result::add);

        result.sort(Comparator.comparing(accuracy -> accuracy.getQuestionType().name()));
        return result;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<SubmissionTopicTagAccuracy> captureTopicTagAccuracies() {
        ArgumentCaptor<Iterable<SubmissionTopicTagAccuracy>> captor =
                ArgumentCaptor.forClass((Class) Iterable.class);

        verify(submissionTopicTagAccuracyRepository).saveAll(captor.capture());

        List<SubmissionTopicTagAccuracy> result = new ArrayList<>();
        captor.getValue().forEach(result::add);

        result.sort(Comparator.comparing(accuracy -> accuracy.getTopicTag().name()));
        return result;
    }

    private SubmissionQuestionTypeAccuracy findTypeAccuracy(List<SubmissionQuestionTypeAccuracy> accuracies,
                                                            PracticeQuestionType questionType) {
        return accuracies.stream()
                .filter(accuracy -> accuracy.getQuestionType() == questionType)
                .findFirst()
                .orElseThrow();
    }

    private SubmissionTopicTagAccuracy findTopicTagAccuracy(List<SubmissionTopicTagAccuracy> accuracies,
                                                            PracticeTopicTag topicTag) {
        return accuracies.stream()
                .filter(accuracy -> accuracy.getTopicTag() == topicTag)
                .findFirst()
                .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private <T> T invokePrivateMethod(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = UserPracticeSubmissionAnswerServiceImpl.class.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return (T) method.invoke(answerService, args);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to invoke " + methodName, exception);
        }
    }

    private void setIfPresent(Object target, String setterName, Object value) {
        Method setter = Arrays.stream(target.getClass().getMethods())
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
