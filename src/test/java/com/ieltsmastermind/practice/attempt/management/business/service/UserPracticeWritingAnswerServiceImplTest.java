package com.ieltsmastermind.practice.attempt.management.business.service;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeWritingAnswerCreateRequestDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeWritingAnswerResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeSubmission;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeWritingAnswer;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeSubmissionRepository;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeWritingAnswerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
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
class UserPracticeWritingAnswerServiceImplTest {

    @Mock
    private UserPracticeSubmissionRepository submissionRepository;

    @Mock
    private UserPracticeWritingAnswerRepository writingAnswerRepository;

    @Mock
    private IncludeSpec includes;

    @InjectMocks
    private UserPracticeWritingAnswerServiceImpl writingAnswerService;

    @Test
    void create_whenSubmissionExistsAndEssayTextHasWords_shouldSaveWritingAnswerWithWordCountAndReturnAnswerId() {
        UserPracticeSubmission submission = submission("submission-1");
        UserPracticeWritingAnswerCreateRequestDto request = createRequest();

        when(submissionRepository.findById("submission-1")).thenReturn(Optional.of(submission));
        when(writingAnswerRepository.save(any(UserPracticeWritingAnswer.class))).thenAnswer(invocation -> {
            UserPracticeWritingAnswer writingAnswer = invocation.getArgument(0);
            writingAnswer.setId("writing-answer-1");
            return writingAnswer;
        });

        UserPracticeWritingAnswerResponseDto result = writingAnswerService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("writing-answer-1");

        ArgumentCaptor<UserPracticeWritingAnswer> writingAnswerCaptor =
                ArgumentCaptor.forClass(UserPracticeWritingAnswer.class);
        verify(writingAnswerRepository).save(writingAnswerCaptor.capture());

        UserPracticeWritingAnswer savedWritingAnswer = writingAnswerCaptor.getValue();

        assertThat(savedWritingAnswer.getSubmission()).isSameAs(submission);
        assertThat(savedWritingAnswer.getOrderIndex()).isEqualTo(1);
        assertThat(savedWritingAnswer.getEssayText()).isEqualTo("This is a sample IELTS writing answer.");
        assertThat(savedWritingAnswer.getWordCount()).isEqualTo(7);

        verify(submissionRepository).findById("submission-1");
    }

    @Test
    void create_whenEssayTextHasExtraSpaces_shouldTrimAndCountWordsCorrectly() {
        UserPracticeSubmission submission = submission("submission-1");
        UserPracticeWritingAnswerCreateRequestDto request = createRequest();
        request.setEssayText("   This   answer   has     extra spaces.   ");

        when(submissionRepository.findById("submission-1")).thenReturn(Optional.of(submission));
        when(writingAnswerRepository.save(any(UserPracticeWritingAnswer.class))).thenAnswer(invocation -> {
            UserPracticeWritingAnswer writingAnswer = invocation.getArgument(0);
            writingAnswer.setId("writing-answer-1");
            return writingAnswer;
        });

        UserPracticeWritingAnswerResponseDto result = writingAnswerService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("writing-answer-1");

        ArgumentCaptor<UserPracticeWritingAnswer> writingAnswerCaptor =
                ArgumentCaptor.forClass(UserPracticeWritingAnswer.class);
        verify(writingAnswerRepository).save(writingAnswerCaptor.capture());

        UserPracticeWritingAnswer savedWritingAnswer = writingAnswerCaptor.getValue();

        assertThat(savedWritingAnswer.getEssayText()).isEqualTo("   This   answer   has     extra spaces.   ");
        assertThat(savedWritingAnswer.getWordCount()).isEqualTo(5);

        verify(submissionRepository).findById("submission-1");
    }

    @Test
    void create_whenEssayTextIsNull_shouldSaveWritingAnswerWithZeroWordCount() {
        UserPracticeSubmission submission = submission("submission-1");
        UserPracticeWritingAnswerCreateRequestDto request = createRequest();
        request.setEssayText(null);

        when(submissionRepository.findById("submission-1")).thenReturn(Optional.of(submission));
        when(writingAnswerRepository.save(any(UserPracticeWritingAnswer.class))).thenAnswer(invocation -> {
            UserPracticeWritingAnswer writingAnswer = invocation.getArgument(0);
            writingAnswer.setId("writing-answer-1");
            return writingAnswer;
        });

        UserPracticeWritingAnswerResponseDto result = writingAnswerService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("writing-answer-1");

        ArgumentCaptor<UserPracticeWritingAnswer> writingAnswerCaptor =
                ArgumentCaptor.forClass(UserPracticeWritingAnswer.class);
        verify(writingAnswerRepository).save(writingAnswerCaptor.capture());

        UserPracticeWritingAnswer savedWritingAnswer = writingAnswerCaptor.getValue();

        assertThat(savedWritingAnswer.getEssayText()).isNull();
        assertThat(savedWritingAnswer.getWordCount()).isZero();

        verify(submissionRepository).findById("submission-1");
    }

    @Test
    void create_whenEssayTextIsBlank_shouldSaveWritingAnswerWithZeroWordCount() {
        UserPracticeSubmission submission = submission("submission-1");
        UserPracticeWritingAnswerCreateRequestDto request = createRequest();
        request.setEssayText("   ");

        when(submissionRepository.findById("submission-1")).thenReturn(Optional.of(submission));
        when(writingAnswerRepository.save(any(UserPracticeWritingAnswer.class))).thenAnswer(invocation -> {
            UserPracticeWritingAnswer writingAnswer = invocation.getArgument(0);
            writingAnswer.setId("writing-answer-1");
            return writingAnswer;
        });

        UserPracticeWritingAnswerResponseDto result = writingAnswerService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("writing-answer-1");

        ArgumentCaptor<UserPracticeWritingAnswer> writingAnswerCaptor =
                ArgumentCaptor.forClass(UserPracticeWritingAnswer.class);
        verify(writingAnswerRepository).save(writingAnswerCaptor.capture());

        UserPracticeWritingAnswer savedWritingAnswer = writingAnswerCaptor.getValue();

        assertThat(savedWritingAnswer.getEssayText()).isEqualTo("   ");
        assertThat(savedWritingAnswer.getWordCount()).isZero();

        verify(submissionRepository).findById("submission-1");
    }

    @Test
    void create_whenSubmissionDoesNotExist_shouldThrowRuntimeExceptionAndSkipSave() {
        UserPracticeWritingAnswerCreateRequestDto request = createRequest();

        when(submissionRepository.findById("submission-1")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> writingAnswerService.create(request)
        );

        assertThat(exception.getMessage()).isEqualTo("Submission not found");

        verify(submissionRepository).findById("submission-1");
        verify(writingAnswerRepository, never()).save(any(UserPracticeWritingAnswer.class));
    }

    @Test
    void getAllBySubmissionId_whenAnswersExistAndFieldsAreIncluded_shouldReturnDtosWithIncludedFields() {
        String submissionId = "submission-1";
        UserPracticeSubmission submission = submission(submissionId);
        UserPracticeWritingAnswer answer1 =
                writingAnswer("writing-answer-1", submission, 1, "First answer text.", 3);
        UserPracticeWritingAnswer answer2 =
                writingAnswer("writing-answer-2", submission, 2, "Second answer text with more words.", 6);

        when(writingAnswerRepository.findAllBySubmission_IdOrderByOrderIndexAsc(submissionId))
                .thenReturn(List.of(answer1, answer2));
        mockIncludes("submissionid", "orderindex", "essaytext", "wordcount");

        List<UserPracticeWritingAnswerResponseDto> result =
                writingAnswerService.getAllBySubmissionId(submissionId, includes);

        assertThat(result).hasSize(2);

        assertThat(result.get(0).getId()).isEqualTo("writing-answer-1");
        assertThat(result.get(0).getSubmissionId()).isEqualTo(submissionId);
        assertThat(result.get(0).getOrderIndex()).isEqualTo(1);
        assertThat(result.get(0).getEssayText()).isEqualTo("First answer text.");
        assertThat(result.get(0).getWordCount()).isEqualTo(3);

        assertThat(result.get(1).getId()).isEqualTo("writing-answer-2");
        assertThat(result.get(1).getSubmissionId()).isEqualTo(submissionId);
        assertThat(result.get(1).getOrderIndex()).isEqualTo(2);
        assertThat(result.get(1).getEssayText()).isEqualTo("Second answer text with more words.");
        assertThat(result.get(1).getWordCount()).isEqualTo(6);

        verify(writingAnswerRepository).findAllBySubmission_IdOrderByOrderIndexAsc(submissionId);
    }

    @Test
    void getAllBySubmissionId_whenNoFieldsAreIncluded_shouldReturnOnlyIds() {
        String submissionId = "submission-1";
        UserPracticeSubmission submission = submission(submissionId);
        UserPracticeWritingAnswer answer =
                writingAnswer("writing-answer-1", submission, 1, "First answer text.", 3);

        when(writingAnswerRepository.findAllBySubmission_IdOrderByOrderIndexAsc(submissionId))
                .thenReturn(List.of(answer));
        mockIncludes();

        List<UserPracticeWritingAnswerResponseDto> result =
                writingAnswerService.getAllBySubmissionId(submissionId, includes);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("writing-answer-1");
        assertThat(result.get(0).getSubmissionId()).isNull();
        assertThat(result.get(0).getOrderIndex()).isNull();
        assertThat(result.get(0).getEssayText()).isNull();
        assertThat(result.get(0).getWordCount()).isNull();

        verify(writingAnswerRepository).findAllBySubmission_IdOrderByOrderIndexAsc(submissionId);
    }

    @Test
    void getAllBySubmissionId_whenRepositoryReturnsEmptyList_shouldReturnEmptyList() {
        String submissionId = "submission-1";

        when(writingAnswerRepository.findAllBySubmission_IdOrderByOrderIndexAsc(submissionId))
                .thenReturn(List.of());

        List<UserPracticeWritingAnswerResponseDto> result =
                writingAnswerService.getAllBySubmissionId(submissionId, includes);

        assertThat(result).isEmpty();

        verify(writingAnswerRepository).findAllBySubmission_IdOrderByOrderIndexAsc(submissionId);
    }

    @Test
    void getById_whenAnswerExistsAndFieldsAreIncluded_shouldReturnDtoWithIncludedFields() {
        UserPracticeSubmission submission = submission("submission-1");
        UserPracticeWritingAnswer answer =
                writingAnswer("writing-answer-1", submission, 1, "First answer text.", 3);

        when(writingAnswerRepository.findById("writing-answer-1")).thenReturn(Optional.of(answer));
        mockIncludes("submissionid", "orderindex", "essaytext", "wordcount");

        UserPracticeWritingAnswerResponseDto result =
                writingAnswerService.getById("writing-answer-1", includes);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("writing-answer-1");
        assertThat(result.getSubmissionId()).isEqualTo("submission-1");
        assertThat(result.getOrderIndex()).isEqualTo(1);
        assertThat(result.getEssayText()).isEqualTo("First answer text.");
        assertThat(result.getWordCount()).isEqualTo(3);

        verify(writingAnswerRepository).findById("writing-answer-1");
    }

    @Test
    void getById_whenNoFieldsAreIncluded_shouldReturnOnlyId() {
        UserPracticeSubmission submission = submission("submission-1");
        UserPracticeWritingAnswer answer =
                writingAnswer("writing-answer-1", submission, 1, "First answer text.", 3);

        when(writingAnswerRepository.findById("writing-answer-1")).thenReturn(Optional.of(answer));
        mockIncludes();

        UserPracticeWritingAnswerResponseDto result =
                writingAnswerService.getById("writing-answer-1", includes);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("writing-answer-1");
        assertThat(result.getSubmissionId()).isNull();
        assertThat(result.getOrderIndex()).isNull();
        assertThat(result.getEssayText()).isNull();
        assertThat(result.getWordCount()).isNull();

        verify(writingAnswerRepository).findById("writing-answer-1");
    }

    @Test
    void getById_whenAnswerDoesNotExist_shouldThrowRuntimeException() {
        when(writingAnswerRepository.findById("missing-writing-answer")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> writingAnswerService.getById("missing-writing-answer", includes)
        );

        assertThat(exception.getMessage()).isEqualTo("Writing answer not found");

        verify(writingAnswerRepository).findById("missing-writing-answer");
    }

    private UserPracticeWritingAnswerCreateRequestDto createRequest() {
        UserPracticeWritingAnswerCreateRequestDto request = new UserPracticeWritingAnswerCreateRequestDto();

        request.setUserPracticeSubmissionId("submission-1");
        request.setOrderIndex(1);
        request.setEssayText("This is a sample IELTS writing answer.");

        return request;
    }

    private UserPracticeSubmission submission(String id) {
        UserPracticeSubmission submission = new UserPracticeSubmission();

        submission.setId(id);

        return submission;
    }

    private UserPracticeWritingAnswer writingAnswer(String id,
                                                    UserPracticeSubmission submission,
                                                    Integer orderIndex,
                                                    String essayText,
                                                    Integer wordCount) {
        UserPracticeWritingAnswer answer = new UserPracticeWritingAnswer();

        answer.setId(id);
        answer.setSubmission(submission);
        answer.setOrderIndex(orderIndex);
        answer.setEssayText(essayText);
        answer.setWordCount(wordCount);

        return answer;
    }

    private void mockIncludes(String... fieldsToInclude) {
        Set<String> includedFields = Set.copyOf(Arrays.asList(fieldsToInclude));

        when(includes.has(anyString())).thenAnswer(invocation -> {
            String fieldName = invocation.getArgument(0);
            return includedFields.contains(fieldName);
        });
    }
}
