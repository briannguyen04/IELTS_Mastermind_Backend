package com.ieltsmastermind.practice.attempt.management.business.service;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeWritingCriterionFeedbackCreateRequestDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeWritingCriterionFeedbackResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeWritingCriterionFeedbackUpdateRequestDto;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeWritingAnswer;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeWritingCriterionFeedback;
import com.ieltsmastermind.practice.attempt.management.domain.enums.FeedbackAuthorType;
import com.ieltsmastermind.practice.attempt.management.domain.enums.WritingCriterionName;
import com.ieltsmastermind.practice.attempt.management.domain.enums.WritingFeedbackLabel;
import com.ieltsmastermind.practice.attempt.management.domain.enums.WritingFeedbackType;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeWritingAnswerRepository;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeWritingCriterionFeedbackRepository;
import com.ieltsmastermind.user.management.domain.entity.User;
import com.ieltsmastermind.user.management.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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
class UserPracticeWritingCriterionFeedbackServiceImplTest {

    @Mock
    private UserPracticeWritingCriterionFeedbackRepository userPracticeWritingCriterionFeedbackRepository;

    @Mock
    private UserPracticeWritingAnswerRepository userPracticeWritingAnswerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IncludeSpec includes;

    @InjectMocks
    private UserPracticeWritingCriterionFeedbackServiceImpl feedbackService;

    @Test
    void create_whenWritingAnswerAndReviewedByUserExist_shouldSaveFeedbackAndReturnFeedbackId() {
        UserPracticeWritingCriterionFeedbackCreateRequestDto request = createRequest();
        UserPracticeWritingAnswer writingAnswer = writingAnswer("writing-answer-1");
        User reviewedByUser = reviewedByUser("reviewer-1");

        when(userPracticeWritingAnswerRepository.findById("writing-answer-1"))
                .thenReturn(Optional.of(writingAnswer));
        when(userRepository.findById("reviewer-1")).thenReturn(Optional.of(reviewedByUser));
        when(userPracticeWritingCriterionFeedbackRepository.save(any(UserPracticeWritingCriterionFeedback.class)))
                .thenAnswer(invocation -> {
                    UserPracticeWritingCriterionFeedback feedback = invocation.getArgument(0);
                    feedback.setId("feedback-1");
                    return feedback;
                });

        UserPracticeWritingCriterionFeedbackResponseDto result = feedbackService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("feedback-1");

        ArgumentCaptor<UserPracticeWritingCriterionFeedback> feedbackCaptor =
                ArgumentCaptor.forClass(UserPracticeWritingCriterionFeedback.class);
        verify(userPracticeWritingCriterionFeedbackRepository).save(feedbackCaptor.capture());

        UserPracticeWritingCriterionFeedback savedFeedback = feedbackCaptor.getValue();

        assertThat(savedFeedback.getAuthorType()).isEqualTo(FeedbackAuthorType.AI);
        assertThat(savedFeedback.getCriterionName()).isEqualTo(WritingCriterionName.TASK_RESPONSE);
        assertThat(savedFeedback.getFeedbackType()).isEqualTo(WritingFeedbackType.STRENGTH);
        assertThat(savedFeedback.getLabel()).isEqualTo(WritingFeedbackLabel.CLEAR_POSITION);
        assertThat(savedFeedback.getDescription()).isEqualTo("The position is clear.");
        assertThat(savedFeedback.getExplanation()).isEqualTo("The essay maintains a consistent opinion.");
        assertThat(savedFeedback.getEvidenceSentences()).containsExactly("I strongly agree with this view.");
        assertThat(savedFeedback.getRecommendedActionDescription()).isEqualTo("Keep your position clear.");
        assertThat(savedFeedback.getRecommendedActionExplanation()).isEqualTo("A clear position improves task response.");
        assertThat(savedFeedback.getWritingAnswer()).isSameAs(writingAnswer);
        assertThat(savedFeedback.getReviewedByUser()).isSameAs(reviewedByUser);

        verify(userPracticeWritingAnswerRepository).findById("writing-answer-1");
        verify(userRepository).findById("reviewer-1");
    }

    @Test
    void create_whenReviewedByUserIdIsNullAndEvidenceSentencesAreNull_shouldSaveFeedbackWithNullReviewerAndEmptyEvidenceSentences() {
        UserPracticeWritingCriterionFeedbackCreateRequestDto request = createRequest();
        request.setReviewedByUserId(null);
        request.setEvidenceSentences(null);

        UserPracticeWritingAnswer writingAnswer = writingAnswer("writing-answer-1");

        when(userPracticeWritingAnswerRepository.findById("writing-answer-1"))
                .thenReturn(Optional.of(writingAnswer));
        when(userPracticeWritingCriterionFeedbackRepository.save(any(UserPracticeWritingCriterionFeedback.class)))
                .thenAnswer(invocation -> {
                    UserPracticeWritingCriterionFeedback feedback = invocation.getArgument(0);
                    feedback.setId("feedback-1");
                    return feedback;
                });

        UserPracticeWritingCriterionFeedbackResponseDto result = feedbackService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("feedback-1");

        ArgumentCaptor<UserPracticeWritingCriterionFeedback> feedbackCaptor =
                ArgumentCaptor.forClass(UserPracticeWritingCriterionFeedback.class);
        verify(userPracticeWritingCriterionFeedbackRepository).save(feedbackCaptor.capture());

        UserPracticeWritingCriterionFeedback savedFeedback = feedbackCaptor.getValue();

        assertThat(savedFeedback.getReviewedByUser()).isNull();
        assertThat(savedFeedback.getEvidenceSentences()).isEmpty();
        assertThat(savedFeedback.getWritingAnswer()).isSameAs(writingAnswer);

        verify(userRepository, never()).findById(anyString());
    }

    @Test
    void create_whenReviewedByUserIdIsBlank_shouldSkipReviewedByUserLookupAndSaveNullReviewer() {
        UserPracticeWritingCriterionFeedbackCreateRequestDto request = createRequest();
        request.setReviewedByUserId("   ");

        UserPracticeWritingAnswer writingAnswer = writingAnswer("writing-answer-1");

        when(userPracticeWritingAnswerRepository.findById("writing-answer-1"))
                .thenReturn(Optional.of(writingAnswer));
        when(userPracticeWritingCriterionFeedbackRepository.save(any(UserPracticeWritingCriterionFeedback.class)))
                .thenAnswer(invocation -> {
                    UserPracticeWritingCriterionFeedback feedback = invocation.getArgument(0);
                    feedback.setId("feedback-1");
                    return feedback;
                });

        UserPracticeWritingCriterionFeedbackResponseDto result = feedbackService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("feedback-1");

        ArgumentCaptor<UserPracticeWritingCriterionFeedback> feedbackCaptor =
                ArgumentCaptor.forClass(UserPracticeWritingCriterionFeedback.class);
        verify(userPracticeWritingCriterionFeedbackRepository).save(feedbackCaptor.capture());

        assertThat(feedbackCaptor.getValue().getReviewedByUser()).isNull();

        verify(userRepository, never()).findById(anyString());
    }

    @Test
    void create_whenWritingAnswerDoesNotExist_shouldThrowRuntimeExceptionAndSkipUserLookupAndSave() {
        UserPracticeWritingCriterionFeedbackCreateRequestDto request = createRequest();

        when(userPracticeWritingAnswerRepository.findById("writing-answer-1"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> feedbackService.create(request)
        );

        assertThat(exception.getMessage()).isEqualTo("Writing answer not found");

        verify(userPracticeWritingAnswerRepository).findById("writing-answer-1");
        verify(userRepository, never()).findById(anyString());
        verify(userPracticeWritingCriterionFeedbackRepository, never())
                .save(any(UserPracticeWritingCriterionFeedback.class));
    }

    @Test
    void create_whenReviewedByUserDoesNotExist_shouldThrowRuntimeExceptionAndSkipSave() {
        UserPracticeWritingCriterionFeedbackCreateRequestDto request = createRequest();
        UserPracticeWritingAnswer writingAnswer = writingAnswer("writing-answer-1");

        when(userPracticeWritingAnswerRepository.findById("writing-answer-1"))
                .thenReturn(Optional.of(writingAnswer));
        when(userRepository.findById("reviewer-1")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> feedbackService.create(request)
        );

        assertThat(exception.getMessage()).isEqualTo("Reviewed by user not found");

        verify(userPracticeWritingAnswerRepository).findById("writing-answer-1");
        verify(userRepository).findById("reviewer-1");
        verify(userPracticeWritingCriterionFeedbackRepository, never())
                .save(any(UserPracticeWritingCriterionFeedback.class));
    }

    @Test
    void getAllBySubmissionId_whenFeedbacksExistAndFieldsAreIncluded_shouldReturnDtosWithIncludedFields() {
        UserPracticeWritingCriterionFeedback feedback = fullFeedback("feedback-1");

        when(userPracticeWritingCriterionFeedbackRepository.findAllBySubmissionIdOrderByCreatedAtAsc("submission-1"))
                .thenReturn(List.of(feedback));
        mockIncludes(
                "authortype",
                "criterionname",
                "feedbacktype",
                "label",
                "description",
                "explanation",
                "evidencesentences",
                "recommendedactiondescription",
                "recommendedactionexplanation",
                "createdat",
                "updatedat",
                "reviewedbyuser.userid",
                "reviewedbyuser.email",
                "reviewedbyuser.firstname",
                "reviewedbyuser.lastname"
        );

        List<UserPracticeWritingCriterionFeedbackResponseDto> result =
                feedbackService.getAllBySubmissionId("submission-1", includes);

        assertThat(result).hasSize(1);

        UserPracticeWritingCriterionFeedbackResponseDto dto = result.get(0);

        assertThat(dto.getId()).isEqualTo("feedback-1");
        assertThat(dto.getAuthorType()).isEqualTo(FeedbackAuthorType.TUTOR);
        assertThat(dto.getCriterionName()).isEqualTo(WritingCriterionName.COHERENCE_AND_COHESION);
        assertThat(dto.getFeedbackType()).isEqualTo(WritingFeedbackType.WEAKNESS);
        assertThat(dto.getLabel()).isEqualTo(WritingFeedbackLabel.WEAK_ORGANISATION);
        assertThat(dto.getDescription()).isEqualTo("Organisation needs improvement.");
        assertThat(dto.getExplanation()).isEqualTo("Ideas are not logically sequenced.");
        assertThat(dto.getEvidenceSentences()).containsExactly("Firstly, the environment is important.");
        assertThat(dto.getRecommendedActionDescription()).isEqualTo("Improve paragraph order.");
        assertThat(dto.getRecommendedActionExplanation()).isEqualTo("Logical order improves coherence.");
        assertThat(dto.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 0));
        assertThat(dto.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 2, 10, 0));

        assertThat(dto.getReviewedByUser()).isNotNull();
        assertThat(dto.getReviewedByUser().getUserId()).isEqualTo("reviewer-1");
        assertThat(dto.getReviewedByUser().getEmail()).isEqualTo("reviewer@example.com");
        assertThat(dto.getReviewedByUser().getFirstname()).isEqualTo("Review");
        assertThat(dto.getReviewedByUser().getLastname()).isEqualTo("Tutor");

        verify(userPracticeWritingCriterionFeedbackRepository)
                .findAllBySubmissionIdOrderByCreatedAtAsc("submission-1");
    }

    @Test
    void getAllBySubmissionId_whenNoFieldsAreIncluded_shouldReturnOnlyFeedbackIdAndEmptyReviewedByUserDto() {
        UserPracticeWritingCriterionFeedback feedback = fullFeedback("feedback-1");

        when(userPracticeWritingCriterionFeedbackRepository.findAllBySubmissionIdOrderByCreatedAtAsc("submission-1"))
                .thenReturn(List.of(feedback));
        mockIncludes();

        List<UserPracticeWritingCriterionFeedbackResponseDto> result =
                feedbackService.getAllBySubmissionId("submission-1", includes);

        assertThat(result).hasSize(1);

        UserPracticeWritingCriterionFeedbackResponseDto dto = result.get(0);

        assertThat(dto.getId()).isEqualTo("feedback-1");
        assertThat(dto.getAuthorType()).isNull();
        assertThat(dto.getCriterionName()).isNull();
        assertThat(dto.getFeedbackType()).isNull();
        assertThat(dto.getLabel()).isNull();
        assertThat(dto.getDescription()).isNull();
        assertThat(dto.getReviewedByUser()).isNotNull();
        assertThat(dto.getReviewedByUser().getUserId()).isNull();
        assertThat(dto.getReviewedByUser().getEmail()).isNull();
        assertThat(dto.getReviewedByUser().getFirstname()).isNull();
        assertThat(dto.getReviewedByUser().getLastname()).isNull();

        verify(userPracticeWritingCriterionFeedbackRepository)
                .findAllBySubmissionIdOrderByCreatedAtAsc("submission-1");
    }

    @Test
    void getAllBySubmissionId_whenReviewedByUserIsNullAndNestedFieldsAreIncluded_shouldReturnEmptyReviewedByUserDto() {
        UserPracticeWritingCriterionFeedback feedback = fullFeedback("feedback-1");
        feedback.setReviewedByUser(null);

        when(userPracticeWritingCriterionFeedbackRepository.findAllBySubmissionIdOrderByCreatedAtAsc("submission-1"))
                .thenReturn(List.of(feedback));
        mockIncludes(
                "reviewedbyuser.userid",
                "reviewedbyuser.email",
                "reviewedbyuser.firstname",
                "reviewedbyuser.lastname"
        );

        List<UserPracticeWritingCriterionFeedbackResponseDto> result =
                feedbackService.getAllBySubmissionId("submission-1", includes);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("feedback-1");
        assertThat(result.get(0).getReviewedByUser()).isNotNull();
        assertThat(result.get(0).getReviewedByUser().getUserId()).isNull();
        assertThat(result.get(0).getReviewedByUser().getEmail()).isNull();
        assertThat(result.get(0).getReviewedByUser().getFirstname()).isNull();
        assertThat(result.get(0).getReviewedByUser().getLastname()).isNull();

        verify(userPracticeWritingCriterionFeedbackRepository)
                .findAllBySubmissionIdOrderByCreatedAtAsc("submission-1");
    }

    @Test
    void getAllBySubmissionId_whenRepositoryReturnsEmptyList_shouldReturnEmptyList() {
        when(userPracticeWritingCriterionFeedbackRepository.findAllBySubmissionIdOrderByCreatedAtAsc("submission-1"))
                .thenReturn(List.of());

        List<UserPracticeWritingCriterionFeedbackResponseDto> result =
                feedbackService.getAllBySubmissionId("submission-1", includes);

        assertThat(result).isEmpty();

        verify(userPracticeWritingCriterionFeedbackRepository)
                .findAllBySubmissionIdOrderByCreatedAtAsc("submission-1");
    }

    @Test
    void update_whenFeedbackExistsAndAllFieldsProvided_shouldUpdateFieldsForceTutorAuthorTypeSaveAndReturnId() {
        UserPracticeWritingCriterionFeedback feedback = fullFeedback("feedback-1");
        feedback.setAuthorType(FeedbackAuthorType.AI);
        UserPracticeWritingCriterionFeedbackUpdateRequestDto request = updateRequest();

        when(userPracticeWritingCriterionFeedbackRepository.findById("feedback-1"))
                .thenReturn(Optional.of(feedback));
        when(userPracticeWritingCriterionFeedbackRepository.save(any(UserPracticeWritingCriterionFeedback.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserPracticeWritingCriterionFeedbackResponseDto result =
                feedbackService.update("feedback-1", request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("feedback-1");

        ArgumentCaptor<UserPracticeWritingCriterionFeedback> feedbackCaptor =
                ArgumentCaptor.forClass(UserPracticeWritingCriterionFeedback.class);
        verify(userPracticeWritingCriterionFeedbackRepository).save(feedbackCaptor.capture());

        UserPracticeWritingCriterionFeedback savedFeedback = feedbackCaptor.getValue();

        assertThat(savedFeedback.getLabel()).isEqualTo(WritingFeedbackLabel.LOGICAL_ORGANISATION);
        assertThat(savedFeedback.getDescription()).isEqualTo("Updated description.");
        assertThat(savedFeedback.getExplanation()).isEqualTo("Updated explanation.");
        assertThat(savedFeedback.getEvidenceSentences()).containsExactly("Updated evidence.");
        assertThat(savedFeedback.getRecommendedActionDescription()).isEqualTo("Updated action.");
        assertThat(savedFeedback.getRecommendedActionExplanation()).isEqualTo("Updated action explanation.");
        assertThat(savedFeedback.getAuthorType()).isEqualTo(FeedbackAuthorType.TUTOR);

        verify(userPracticeWritingCriterionFeedbackRepository).findById("feedback-1");
    }

    @Test
    void update_whenRequestContainsOnlyNullFields_shouldKeepExistingValuesForceTutorAuthorTypeAndStillSave() {
        UserPracticeWritingCriterionFeedback feedback = fullFeedback("feedback-1");
        feedback.setAuthorType(FeedbackAuthorType.AI);
        UserPracticeWritingCriterionFeedbackUpdateRequestDto request =
                new UserPracticeWritingCriterionFeedbackUpdateRequestDto();

        when(userPracticeWritingCriterionFeedbackRepository.findById("feedback-1"))
                .thenReturn(Optional.of(feedback));
        when(userPracticeWritingCriterionFeedbackRepository.save(any(UserPracticeWritingCriterionFeedback.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserPracticeWritingCriterionFeedbackResponseDto result =
                feedbackService.update("feedback-1", request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("feedback-1");

        assertThat(feedback.getLabel()).isEqualTo(WritingFeedbackLabel.WEAK_ORGANISATION);
        assertThat(feedback.getDescription()).isEqualTo("Organisation needs improvement.");
        assertThat(feedback.getExplanation()).isEqualTo("Ideas are not logically sequenced.");
        assertThat(feedback.getEvidenceSentences()).containsExactly("Firstly, the environment is important.");
        assertThat(feedback.getRecommendedActionDescription()).isEqualTo("Improve paragraph order.");
        assertThat(feedback.getRecommendedActionExplanation()).isEqualTo("Logical order improves coherence.");
        assertThat(feedback.getAuthorType()).isEqualTo(FeedbackAuthorType.TUTOR);

        verify(userPracticeWritingCriterionFeedbackRepository).findById("feedback-1");
        verify(userPracticeWritingCriterionFeedbackRepository).save(feedback);
    }

    @Test
    void update_whenEvidenceSentencesIsEmptyList_shouldReplaceEvidenceSentencesWithEmptyList() {
        UserPracticeWritingCriterionFeedback feedback = fullFeedback("feedback-1");
        UserPracticeWritingCriterionFeedbackUpdateRequestDto request =
                new UserPracticeWritingCriterionFeedbackUpdateRequestDto();
        request.setEvidenceSentences(List.of());

        when(userPracticeWritingCriterionFeedbackRepository.findById("feedback-1"))
                .thenReturn(Optional.of(feedback));
        when(userPracticeWritingCriterionFeedbackRepository.save(any(UserPracticeWritingCriterionFeedback.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserPracticeWritingCriterionFeedbackResponseDto result =
                feedbackService.update("feedback-1", request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("feedback-1");
        assertThat(feedback.getEvidenceSentences()).isEmpty();
        assertThat(feedback.getAuthorType()).isEqualTo(FeedbackAuthorType.TUTOR);

        verify(userPracticeWritingCriterionFeedbackRepository).save(feedback);
    }

    @Test
    void update_whenFeedbackDoesNotExist_shouldThrowRuntimeExceptionAndSkipSave() {
        UserPracticeWritingCriterionFeedbackUpdateRequestDto request = updateRequest();

        when(userPracticeWritingCriterionFeedbackRepository.findById("missing-feedback"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> feedbackService.update("missing-feedback", request)
        );

        assertThat(exception.getMessage())
                .isEqualTo("User practice writing criterion feedback not found with id: missing-feedback");

        verify(userPracticeWritingCriterionFeedbackRepository).findById("missing-feedback");
        verify(userPracticeWritingCriterionFeedbackRepository, never())
                .save(any(UserPracticeWritingCriterionFeedback.class));
    }

    @Test
    void delete_whenFeedbackExists_shouldDeleteFeedback() {
        UserPracticeWritingCriterionFeedback feedback = fullFeedback("feedback-1");

        when(userPracticeWritingCriterionFeedbackRepository.findById("feedback-1"))
                .thenReturn(Optional.of(feedback));

        feedbackService.delete("feedback-1");

        verify(userPracticeWritingCriterionFeedbackRepository).findById("feedback-1");
        verify(userPracticeWritingCriterionFeedbackRepository).delete(feedback);
    }

    @Test
    void delete_whenFeedbackDoesNotExist_shouldThrowRuntimeExceptionAndSkipDelete() {
        when(userPracticeWritingCriterionFeedbackRepository.findById("missing-feedback"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> feedbackService.delete("missing-feedback")
        );

        assertThat(exception.getMessage())
                .isEqualTo("User practice writing criterion feedback not found with id: missing-feedback");

        verify(userPracticeWritingCriterionFeedbackRepository).findById("missing-feedback");
        verify(userPracticeWritingCriterionFeedbackRepository, never())
                .delete(any(UserPracticeWritingCriterionFeedback.class));
    }

    private UserPracticeWritingCriterionFeedbackCreateRequestDto createRequest() {
        UserPracticeWritingCriterionFeedbackCreateRequestDto request =
                new UserPracticeWritingCriterionFeedbackCreateRequestDto();

        request.setAuthorType(FeedbackAuthorType.AI);
        request.setCriterionName(WritingCriterionName.TASK_RESPONSE);
        request.setFeedbackType(WritingFeedbackType.STRENGTH);
        request.setLabel(WritingFeedbackLabel.CLEAR_POSITION);
        request.setDescription("The position is clear.");
        request.setExplanation("The essay maintains a consistent opinion.");
        request.setEvidenceSentences(List.of("I strongly agree with this view."));
        request.setRecommendedActionDescription("Keep your position clear.");
        request.setRecommendedActionExplanation("A clear position improves task response.");
        request.setUserPracticeWritingAnswerId("writing-answer-1");
        request.setReviewedByUserId("reviewer-1");

        return request;
    }

    private UserPracticeWritingCriterionFeedbackUpdateRequestDto updateRequest() {
        UserPracticeWritingCriterionFeedbackUpdateRequestDto request =
                new UserPracticeWritingCriterionFeedbackUpdateRequestDto();

        request.setLabel(WritingFeedbackLabel.LOGICAL_ORGANISATION);
        request.setDescription("Updated description.");
        request.setExplanation("Updated explanation.");
        request.setEvidenceSentences(List.of("Updated evidence."));
        request.setRecommendedActionDescription("Updated action.");
        request.setRecommendedActionExplanation("Updated action explanation.");

        return request;
    }

    private UserPracticeWritingAnswer writingAnswer(String id) {
        UserPracticeWritingAnswer writingAnswer = new UserPracticeWritingAnswer();

        writingAnswer.setId(id);
        writingAnswer.setOrderIndex(1);
        writingAnswer.setEssayText("This is a sample essay.");
        writingAnswer.setWordCount(5);

        return writingAnswer;
    }

    private UserPracticeWritingCriterionFeedback fullFeedback(String id) {
        UserPracticeWritingCriterionFeedback feedback = new UserPracticeWritingCriterionFeedback();

        feedback.setId(id);
        feedback.setAuthorType(FeedbackAuthorType.TUTOR);
        feedback.setCriterionName(WritingCriterionName.COHERENCE_AND_COHESION);
        feedback.setFeedbackType(WritingFeedbackType.WEAKNESS);
        feedback.setLabel(WritingFeedbackLabel.WEAK_ORGANISATION);
        feedback.setDescription("Organisation needs improvement.");
        feedback.setExplanation("Ideas are not logically sequenced.");
        feedback.setEvidenceSentences(List.of("Firstly, the environment is important."));
        feedback.setRecommendedActionDescription("Improve paragraph order.");
        feedback.setRecommendedActionExplanation("Logical order improves coherence.");
        feedback.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        feedback.setUpdatedAt(LocalDateTime.of(2026, 1, 2, 10, 0));
        feedback.setWritingAnswer(writingAnswer("writing-answer-1"));
        feedback.setReviewedByUser(reviewedByUser("reviewer-1"));

        return feedback;
    }

    private User reviewedByUser(String userId) {
        User user = new User();

        user.setUserId(userId);
        user.setEmail("reviewer@example.com");
        user.setFirstname("Review");
        user.setLastname("Tutor");

        return user;
    }

    private void mockIncludes(String... fieldsToInclude) {
        Set<String> includedFields = Set.copyOf(Arrays.asList(fieldsToInclude));

        when(includes.has(anyString())).thenAnswer(invocation -> {
            String fieldName = invocation.getArgument(0);
            return includedFields.contains(fieldName);
        });
    }
}
