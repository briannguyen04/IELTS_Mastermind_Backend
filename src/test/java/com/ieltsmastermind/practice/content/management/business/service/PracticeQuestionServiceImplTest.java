package com.ieltsmastermind.practice.content.management.business.service;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.content.management.domain.dto.PracticeQuestionCreateRequestDto;
import com.ieltsmastermind.practice.content.management.domain.dto.PracticeQuestionResponseDto;
import com.ieltsmastermind.practice.content.management.domain.dto.PracticeQuestionUpdateRequestDto;
import com.ieltsmastermind.practice.content.management.domain.entity.PracticeContent;
import com.ieltsmastermind.practice.content.management.domain.entity.PracticeQuestion;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTopicTag;
import com.ieltsmastermind.practice.content.management.persistence.PracticeContentRepository;
import com.ieltsmastermind.practice.content.management.persistence.PracticeQuestionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
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
class PracticeQuestionServiceImplTest {

    @Mock
    private PracticeQuestionRepository practiceQuestionRepository;

    @Mock
    private PracticeContentRepository practiceContentRepository;

    @Mock
    private IncludeSpec includes;

    @InjectMocks
    private PracticeQuestionServiceImpl practiceQuestionService;

    @Test
    void create_whenPracticeContentExistsAndRequestIsValid_shouldSaveQuestionAndReturnQuestionId() {
        String practiceContentId = "content-1";
        PracticeContent content = practiceContent(practiceContentId);
        PracticeQuestionCreateRequestDto request = createRequest();

        when(practiceContentRepository.findById(practiceContentId)).thenReturn(Optional.of(content));
        when(practiceQuestionRepository.save(any(PracticeQuestion.class))).thenAnswer(invocation -> {
            PracticeQuestion question = invocation.getArgument(0);
            setIfPresent(question, "setId", "question-1");
            return question;
        });

        PracticeQuestionResponseDto result = practiceQuestionService.create(practiceContentId, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("question-1");

        ArgumentCaptor<PracticeQuestion> questionCaptor = ArgumentCaptor.forClass(PracticeQuestion.class);
        verify(practiceQuestionRepository).save(questionCaptor.capture());

        PracticeQuestion savedQuestion = questionCaptor.getValue();

        assertThat(savedQuestion.getPracticeContent()).isSameAs(content);
        assertThat(savedQuestion.getOrderIndex()).isEqualTo(1);
        assertThat(savedQuestion.getType()).isEqualTo(PracticeQuestionType.MULTIPLE_CHOICE);
        assertThat(savedQuestion.getTopicTag()).isEqualTo(PracticeTopicTag.EDUCATION_AND_LEARNING);
        assertThat(savedQuestion.getCorrectAnswers()).containsExactly("A", "B");
        assertThat(savedQuestion.getCorrectAnswers()).isNotSameAs(request.getCorrectAnswers());

        verify(practiceContentRepository).findById(practiceContentId);
        verify(practiceQuestionRepository).save(any(PracticeQuestion.class));
    }

    @Test
    void create_whenCorrectAnswersIsNull_shouldSaveEmptyCorrectAnswers() {
        String practiceContentId = "content-1";
        PracticeContent content = practiceContent(practiceContentId);
        PracticeQuestionCreateRequestDto request = createRequest();
        request.setCorrectAnswers(null);

        when(practiceContentRepository.findById(practiceContentId)).thenReturn(Optional.of(content));
        when(practiceQuestionRepository.save(any(PracticeQuestion.class))).thenAnswer(invocation -> {
            PracticeQuestion question = invocation.getArgument(0);
            setIfPresent(question, "setId", "question-1");
            return question;
        });

        PracticeQuestionResponseDto result = practiceQuestionService.create(practiceContentId, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("question-1");

        ArgumentCaptor<PracticeQuestion> questionCaptor = ArgumentCaptor.forClass(PracticeQuestion.class);
        verify(practiceQuestionRepository).save(questionCaptor.capture());

        PracticeQuestion savedQuestion = questionCaptor.getValue();

        assertThat(savedQuestion.getPracticeContent()).isSameAs(content);
        assertThat(savedQuestion.getCorrectAnswers()).isEmpty();

        verify(practiceContentRepository).findById(practiceContentId);
    }

    @Test
    void create_whenPracticeContentDoesNotExist_shouldThrowRuntimeExceptionAndSkipSave() {
        String practiceContentId = "missing-content";
        PracticeQuestionCreateRequestDto request = createRequest();

        when(practiceContentRepository.findById(practiceContentId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> practiceQuestionService.create(practiceContentId, request)
        );

        assertThat(exception.getMessage()).isEqualTo("Practice content not found");

        verify(practiceContentRepository).findById(practiceContentId);
        verify(practiceQuestionRepository, never()).save(any(PracticeQuestion.class));
    }

    @Test
    void getAllByPracticeContentId_whenContentExistsAndQuestionsExistWithIncludes_shouldReturnQuestionDtosWithIncludedFields() {
        String practiceContentId = "content-1";
        PracticeContent content = practiceContent(practiceContentId);
        PracticeQuestion question1 = fullQuestion("question-1");
        PracticeQuestion question2 = fullQuestion("question-2");
        question2.setOrderIndex(2);
        question2.setType(PracticeQuestionType.MATCHING);
        question2.setTopicTag(PracticeTopicTag.TECHNOLOGY_INTERNET_AND_AI);
        question2.setCorrectAnswers(List.of("C"));

        when(practiceContentRepository.findById(practiceContentId)).thenReturn(Optional.of(content));
        when(practiceQuestionRepository.findByPracticeContent_IdOrderByOrderIndexAsc(practiceContentId))
                .thenReturn(List.of(question1, question2));
        mockIncludes("orderindex", "type", "topictag", "correctanswers");

        List<PracticeQuestionResponseDto> result =
                practiceQuestionService.getAllByPracticeContentId(practiceContentId, includes);

        assertThat(result).hasSize(2);

        assertThat(result.get(0).getId()).isEqualTo("question-1");
        assertThat(result.get(0).getPracticeContentId()).isEqualTo(practiceContentId);
        assertThat(result.get(0).getOrderIndex()).isEqualTo(1);
        assertThat(result.get(0).getType()).isEqualTo(PracticeQuestionType.MULTIPLE_CHOICE);
        assertThat(result.get(0).getTopicTag()).isEqualTo(PracticeTopicTag.EDUCATION_AND_LEARNING);
        assertThat(result.get(0).getCorrectAnswers()).containsExactly("A", "B");

        assertThat(result.get(1).getId()).isEqualTo("question-2");
        assertThat(result.get(1).getPracticeContentId()).isEqualTo(practiceContentId);
        assertThat(result.get(1).getOrderIndex()).isEqualTo(2);
        assertThat(result.get(1).getType()).isEqualTo(PracticeQuestionType.MATCHING);
        assertThat(result.get(1).getTopicTag()).isEqualTo(PracticeTopicTag.TECHNOLOGY_INTERNET_AND_AI);
        assertThat(result.get(1).getCorrectAnswers()).containsExactly("C");

        verify(practiceContentRepository).findById(practiceContentId);
        verify(practiceQuestionRepository).findByPracticeContent_IdOrderByOrderIndexAsc(practiceContentId);
    }

    @Test
    void getAllByPracticeContentId_whenNoFieldsAreIncluded_shouldReturnOnlyIdsAndPracticeContentId() {
        String practiceContentId = "content-1";
        PracticeContent content = practiceContent(practiceContentId);
        PracticeQuestion question = fullQuestion("question-1");

        when(practiceContentRepository.findById(practiceContentId)).thenReturn(Optional.of(content));
        when(practiceQuestionRepository.findByPracticeContent_IdOrderByOrderIndexAsc(practiceContentId))
                .thenReturn(List.of(question));
        mockIncludes();

        List<PracticeQuestionResponseDto> result =
                practiceQuestionService.getAllByPracticeContentId(practiceContentId, includes);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("question-1");
        assertThat(result.get(0).getPracticeContentId()).isEqualTo(practiceContentId);
        assertThat(result.get(0).getOrderIndex()).isNull();
        assertThat(result.get(0).getType()).isNull();
        assertThat(result.get(0).getTopicTag()).isNull();
        assertThat(result.get(0).getCorrectAnswers()).isNull();

        verify(practiceContentRepository).findById(practiceContentId);
        verify(practiceQuestionRepository).findByPracticeContent_IdOrderByOrderIndexAsc(practiceContentId);
    }

    @Test
    void getAllByPracticeContentId_whenRepositoryReturnsEmptyList_shouldReturnEmptyList() {
        String practiceContentId = "content-1";
        PracticeContent content = practiceContent(practiceContentId);

        when(practiceContentRepository.findById(practiceContentId)).thenReturn(Optional.of(content));
        when(practiceQuestionRepository.findByPracticeContent_IdOrderByOrderIndexAsc(practiceContentId))
                .thenReturn(List.of());

        List<PracticeQuestionResponseDto> result =
                practiceQuestionService.getAllByPracticeContentId(practiceContentId, includes);

        assertThat(result).isEmpty();

        verify(practiceContentRepository).findById(practiceContentId);
        verify(practiceQuestionRepository).findByPracticeContent_IdOrderByOrderIndexAsc(practiceContentId);
    }

    @Test
    void getAllByPracticeContentId_whenPracticeContentDoesNotExist_shouldThrowRuntimeExceptionAndSkipQuestionLookup() {
        String practiceContentId = "missing-content";

        when(practiceContentRepository.findById(practiceContentId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> practiceQuestionService.getAllByPracticeContentId(practiceContentId, includes)
        );

        assertThat(exception.getMessage()).isEqualTo("Practice content not found");

        verify(practiceContentRepository).findById(practiceContentId);
        verify(practiceQuestionRepository, never()).findByPracticeContent_IdOrderByOrderIndexAsc(anyString());
    }

    @Test
    void update_whenQuestionExistsAndAllFieldsProvided_shouldUpdateFieldsSaveAndReturnQuestionId() {
        String questionId = "question-1";
        PracticeQuestion existingQuestion = fullQuestion(questionId);
        PracticeQuestionUpdateRequestDto request = fullUpdateRequest();

        when(practiceQuestionRepository.findById(questionId)).thenReturn(Optional.of(existingQuestion));
        when(practiceQuestionRepository.save(any(PracticeQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PracticeQuestionResponseDto result = practiceQuestionService.update(questionId, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(questionId);

        ArgumentCaptor<PracticeQuestion> questionCaptor = ArgumentCaptor.forClass(PracticeQuestion.class);
        verify(practiceQuestionRepository).save(questionCaptor.capture());

        PracticeQuestion savedQuestion = questionCaptor.getValue();

        assertThat(savedQuestion.getOrderIndex()).isEqualTo(2);
        assertThat(savedQuestion.getType()).isEqualTo(PracticeQuestionType.MATCHING);
        assertThat(savedQuestion.getTopicTag()).isEqualTo(PracticeTopicTag.TECHNOLOGY_INTERNET_AND_AI);
        assertThat(savedQuestion.getCorrectAnswers()).containsExactly("C", "D");
        assertThat(savedQuestion.getCorrectAnswers()).isSameAs(request.getCorrectAnswers());

        verify(practiceQuestionRepository).findById(questionId);
    }

    @Test
    void update_whenRequestContainsOnlyNullFields_shouldKeepExistingValuesAndStillSave() {
        String questionId = "question-1";
        PracticeQuestion existingQuestion = fullQuestion(questionId);
        PracticeQuestionUpdateRequestDto request = new PracticeQuestionUpdateRequestDto();

        when(practiceQuestionRepository.findById(questionId)).thenReturn(Optional.of(existingQuestion));
        when(practiceQuestionRepository.save(any(PracticeQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PracticeQuestionResponseDto result = practiceQuestionService.update(questionId, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(questionId);
        assertThat(existingQuestion.getOrderIndex()).isEqualTo(1);
        assertThat(existingQuestion.getType()).isEqualTo(PracticeQuestionType.MULTIPLE_CHOICE);
        assertThat(existingQuestion.getTopicTag()).isEqualTo(PracticeTopicTag.EDUCATION_AND_LEARNING);
        assertThat(existingQuestion.getCorrectAnswers()).containsExactly("A", "B");

        verify(practiceQuestionRepository).findById(questionId);
        verify(practiceQuestionRepository).save(existingQuestion);
    }

    @Test
    void update_whenCorrectAnswersProvidedAsEmptyList_shouldReplaceCorrectAnswersWithEmptyList() {
        String questionId = "question-1";
        PracticeQuestion existingQuestion = fullQuestion(questionId);
        PracticeQuestionUpdateRequestDto request = new PracticeQuestionUpdateRequestDto();
        request.setCorrectAnswers(List.of());

        when(practiceQuestionRepository.findById(questionId)).thenReturn(Optional.of(existingQuestion));
        when(practiceQuestionRepository.save(any(PracticeQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PracticeQuestionResponseDto result = practiceQuestionService.update(questionId, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(questionId);
        assertThat(existingQuestion.getCorrectAnswers()).isEmpty();

        verify(practiceQuestionRepository).findById(questionId);
        verify(practiceQuestionRepository).save(existingQuestion);
    }

    @Test
    void update_whenQuestionDoesNotExist_shouldThrowRuntimeExceptionAndSkipSave() {
        String questionId = "missing-question";
        PracticeQuestionUpdateRequestDto request = fullUpdateRequest();

        when(practiceQuestionRepository.findById(questionId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> practiceQuestionService.update(questionId, request)
        );

        assertThat(exception.getMessage()).isEqualTo("Practice question not found with id: missing-question");

        verify(practiceQuestionRepository).findById(questionId);
        verify(practiceQuestionRepository, never()).save(any(PracticeQuestion.class));
    }

    @Test
    void delete_whenQuestionExists_shouldDeleteQuestion() {
        String questionId = "question-1";
        PracticeQuestion question = fullQuestion(questionId);

        when(practiceQuestionRepository.findById(questionId)).thenReturn(Optional.of(question));

        practiceQuestionService.delete(questionId);

        verify(practiceQuestionRepository).findById(questionId);
        verify(practiceQuestionRepository).delete(question);
    }

    @Test
    void delete_whenQuestionDoesNotExist_shouldThrowRuntimeExceptionAndSkipDelete() {
        String questionId = "missing-question";

        when(practiceQuestionRepository.findById(questionId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> practiceQuestionService.delete(questionId)
        );

        assertThat(exception.getMessage()).isEqualTo("Practice question not found with id: missing-question");

        verify(practiceQuestionRepository).findById(questionId);
        verify(practiceQuestionRepository, never()).delete(any(PracticeQuestion.class));
    }

    private PracticeQuestionCreateRequestDto createRequest() {
        PracticeQuestionCreateRequestDto request = new PracticeQuestionCreateRequestDto();

        request.setOrderIndex(1);
        request.setType(PracticeQuestionType.MULTIPLE_CHOICE);
        request.setTopicTag(PracticeTopicTag.EDUCATION_AND_LEARNING);
        request.setCorrectAnswers(List.of("A", "B"));

        return request;
    }

    private PracticeQuestionUpdateRequestDto fullUpdateRequest() {
        PracticeQuestionUpdateRequestDto request = new PracticeQuestionUpdateRequestDto();

        request.setOrderIndex(2);
        request.setType(PracticeQuestionType.MATCHING);
        request.setTopicTag(PracticeTopicTag.TECHNOLOGY_INTERNET_AND_AI);
        request.setCorrectAnswers(List.of("C", "D"));

        return request;
    }

    private PracticeContent practiceContent(String id) {
        PracticeContent content = new PracticeContent();

        setIfPresent(content, "setId", id);

        return content;
    }

    private PracticeQuestion fullQuestion(String id) {
        PracticeQuestion question = new PracticeQuestion();

        setIfPresent(question, "setId", id);
        question.setOrderIndex(1);
        question.setType(PracticeQuestionType.MULTIPLE_CHOICE);
        question.setTopicTag(PracticeTopicTag.EDUCATION_AND_LEARNING);
        question.setCorrectAnswers(List.of("A", "B"));

        return question;
    }

    private void mockIncludes(String... fieldsToInclude) {
        Set<String> includedFields = Set.copyOf(Arrays.asList(fieldsToInclude));

        when(includes.has(anyString())).thenAnswer(invocation -> {
            String fieldName = invocation.getArgument(0);
            return includedFields.contains(fieldName);
        });
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
