package com.ieltsmastermind.practice.attempt.management.business.service;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.attempt.management.domain.dto.SubmissionFeedbackCreateRequestDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.SubmissionFeedbackResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.SubmissionFeedbackUpdateRequestDto;
import com.ieltsmastermind.practice.attempt.management.domain.entity.SubmissionFeedback;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeSubmission;
import com.ieltsmastermind.practice.attempt.management.domain.enums.FeedbackAuthorType;
import com.ieltsmastermind.practice.attempt.management.persistence.SubmissionFeedbackRepository;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeSubmissionRepository;
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
class SubmissionFeedbackServiceImplTest {

    @Mock
    private SubmissionFeedbackRepository submissionFeedbackRepository;

    @Mock
    private UserPracticeSubmissionRepository userPracticeSubmissionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IncludeSpec includes;

    @InjectMocks
    private SubmissionFeedbackServiceImpl submissionFeedbackService;

    @Test
    void create_whenSubmissionAndAuthorExist_shouldSaveFeedbackAndReturnFeedbackId() {
        SubmissionFeedbackCreateRequestDto request = createRequest();
        UserPracticeSubmission submission = submission("submission-1");
        User author = author("author-1");

        when(userPracticeSubmissionRepository.findById("submission-1"))
                .thenReturn(Optional.of(submission));
        when(userRepository.findById("author-1"))
                .thenReturn(Optional.of(author));
        when(submissionFeedbackRepository.save(any(SubmissionFeedback.class)))
                .thenAnswer(invocation -> {
                    SubmissionFeedback feedback = invocation.getArgument(0);
                    feedback.setId("feedback-1");
                    return feedback;
                });

        SubmissionFeedbackResponseDto result = submissionFeedbackService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("feedback-1");

        ArgumentCaptor<SubmissionFeedback> feedbackCaptor =
                ArgumentCaptor.forClass(SubmissionFeedback.class);
        verify(submissionFeedbackRepository).save(feedbackCaptor.capture());

        SubmissionFeedback savedFeedback = feedbackCaptor.getValue();

        assertThat(savedFeedback.getSubmission()).isSameAs(submission);
        assertThat(savedFeedback.getAuthor()).isSameAs(author);
        assertThat(savedFeedback.getAuthorType()).isEqualTo(FeedbackAuthorType.TUTOR);
        assertThat(savedFeedback.getFeedbackContent()).isEqualTo("Strong feedback content.");

        verify(userPracticeSubmissionRepository).findById("submission-1");
        verify(userRepository).findById("author-1");
    }

    @Test
    void create_whenSubmissionDoesNotExist_shouldThrowIllegalArgumentExceptionAndSkipAuthorLookupAndSave() {
        SubmissionFeedbackCreateRequestDto request = createRequest();

        when(userPracticeSubmissionRepository.findById("submission-1"))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> submissionFeedbackService.create(request)
        );

        assertThat(exception.getMessage()).isEqualTo("UserPracticeSubmission not found: submission-1");

        verify(userPracticeSubmissionRepository).findById("submission-1");
        verify(userRepository, never()).findById(anyString());
        verify(submissionFeedbackRepository, never()).save(any(SubmissionFeedback.class));
    }

    @Test
    void create_whenAuthorDoesNotExist_shouldThrowIllegalArgumentExceptionAndSkipSave() {
        SubmissionFeedbackCreateRequestDto request = createRequest();
        UserPracticeSubmission submission = submission("submission-1");

        when(userPracticeSubmissionRepository.findById("submission-1"))
                .thenReturn(Optional.of(submission));
        when(userRepository.findById("author-1"))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> submissionFeedbackService.create(request)
        );

        assertThat(exception.getMessage()).isEqualTo("User not found: author-1");

        verify(userPracticeSubmissionRepository).findById("submission-1");
        verify(userRepository).findById("author-1");
        verify(submissionFeedbackRepository, never()).save(any(SubmissionFeedback.class));
    }

    @Test
    void getAllBySubmissionId_whenFeedbackRowsExistAndFieldsAreIncluded_shouldReturnDtosWithIncludedFields() {
        String submissionId = "submission-1";
        SubmissionFeedback feedback = fullFeedback("feedback-1");

        when(submissionFeedbackRepository.findAllBySubmissionIdOrderByCreatedAtAsc(submissionId))
                .thenReturn(List.of(feedback));
        mockIncludes(
                "authortype",
                "feedbackcontent",
                "createdat",
                "updatedat",
                "author.id",
                "author.firstname",
                "author.lastname",
                "author.avatarurl"
        );

        List<SubmissionFeedbackResponseDto> result =
                submissionFeedbackService.getAllBySubmissionId(submissionId, includes);

        assertThat(result).hasSize(1);

        SubmissionFeedbackResponseDto dto = result.get(0);

        assertThat(dto.getId()).isEqualTo("feedback-1");
        assertThat(dto.getAuthorType()).isEqualTo(FeedbackAuthorType.TUTOR);
        assertThat(dto.getFeedbackContent()).isEqualTo("Strong feedback content.");
        assertThat(dto.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 0));
        assertThat(dto.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 2, 10, 0));

        assertThat(dto.getAuthor()).isNotNull();
        assertThat(dto.getAuthor().getId()).isEqualTo("author-1");
        assertThat(dto.getAuthor().getFirstname()).isEqualTo("Author");
        assertThat(dto.getAuthor().getLastname()).isEqualTo("User");
        assertThat(dto.getAuthor().getAvatarUrl()).isEqualTo("avatar.png");

        verify(submissionFeedbackRepository).findAllBySubmissionIdOrderByCreatedAtAsc(submissionId);
    }

    @Test
    void getAllBySubmissionId_whenNoFieldsAreIncluded_shouldReturnOnlyIdsAndEmptyAuthorDto() {
        String submissionId = "submission-1";
        SubmissionFeedback feedback = fullFeedback("feedback-1");

        when(submissionFeedbackRepository.findAllBySubmissionIdOrderByCreatedAtAsc(submissionId))
                .thenReturn(List.of(feedback));
        mockIncludes();

        List<SubmissionFeedbackResponseDto> result =
                submissionFeedbackService.getAllBySubmissionId(submissionId, includes);

        assertThat(result).hasSize(1);

        SubmissionFeedbackResponseDto dto = result.get(0);

        assertThat(dto.getId()).isEqualTo("feedback-1");
        assertThat(dto.getAuthorType()).isNull();
        assertThat(dto.getFeedbackContent()).isNull();
        assertThat(dto.getCreatedAt()).isNull();
        assertThat(dto.getUpdatedAt()).isNull();

        assertThat(dto.getAuthor()).isNotNull();
        assertThat(dto.getAuthor().getId()).isNull();
        assertThat(dto.getAuthor().getFirstname()).isNull();
        assertThat(dto.getAuthor().getLastname()).isNull();
        assertThat(dto.getAuthor().getAvatarUrl()).isNull();

        verify(submissionFeedbackRepository).findAllBySubmissionIdOrderByCreatedAtAsc(submissionId);
    }

    @Test
    void getAllBySubmissionId_whenRepositoryReturnsEmptyList_shouldReturnEmptyList() {
        String submissionId = "submission-1";

        when(submissionFeedbackRepository.findAllBySubmissionIdOrderByCreatedAtAsc(submissionId))
                .thenReturn(List.of());

        List<SubmissionFeedbackResponseDto> result =
                submissionFeedbackService.getAllBySubmissionId(submissionId, includes);

        assertThat(result).isEmpty();

        verify(submissionFeedbackRepository).findAllBySubmissionIdOrderByCreatedAtAsc(submissionId);
    }

    @Test
    void update_whenFeedbackExistsAndAllFieldsProvided_shouldUpdateFieldsSaveAndReturnFeedbackId() {
        SubmissionFeedback feedback = fullFeedback("feedback-1");
        SubmissionFeedbackUpdateRequestDto request = updateRequest();

        when(submissionFeedbackRepository.findById("feedback-1"))
                .thenReturn(Optional.of(feedback));
        when(submissionFeedbackRepository.save(any(SubmissionFeedback.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SubmissionFeedbackResponseDto result =
                submissionFeedbackService.update("feedback-1", request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("feedback-1");

        ArgumentCaptor<SubmissionFeedback> feedbackCaptor =
                ArgumentCaptor.forClass(SubmissionFeedback.class);
        verify(submissionFeedbackRepository).save(feedbackCaptor.capture());

        SubmissionFeedback savedFeedback = feedbackCaptor.getValue();

        assertThat(savedFeedback.getAuthorType()).isEqualTo(FeedbackAuthorType.AI);
        assertThat(savedFeedback.getFeedbackContent()).isEqualTo("Updated feedback content.");

        verify(submissionFeedbackRepository).findById("feedback-1");
    }

    @Test
    void update_whenRequestContainsOnlyNullFields_shouldKeepExistingFieldsAndStillSave() {
        SubmissionFeedback feedback = fullFeedback("feedback-1");
        SubmissionFeedbackUpdateRequestDto request = new SubmissionFeedbackUpdateRequestDto();

        when(submissionFeedbackRepository.findById("feedback-1"))
                .thenReturn(Optional.of(feedback));
        when(submissionFeedbackRepository.save(any(SubmissionFeedback.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SubmissionFeedbackResponseDto result =
                submissionFeedbackService.update("feedback-1", request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("feedback-1");

        assertThat(feedback.getAuthorType()).isEqualTo(FeedbackAuthorType.TUTOR);
        assertThat(feedback.getFeedbackContent()).isEqualTo("Strong feedback content.");

        verify(submissionFeedbackRepository).findById("feedback-1");
        verify(submissionFeedbackRepository).save(feedback);
    }

    @Test
    void update_whenOnlyAuthorTypeProvided_shouldUpdateAuthorTypeOnlyAndSave() {
        SubmissionFeedback feedback = fullFeedback("feedback-1");
        SubmissionFeedbackUpdateRequestDto request = new SubmissionFeedbackUpdateRequestDto();
        request.setAuthorType(FeedbackAuthorType.AI);

        when(submissionFeedbackRepository.findById("feedback-1"))
                .thenReturn(Optional.of(feedback));
        when(submissionFeedbackRepository.save(any(SubmissionFeedback.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SubmissionFeedbackResponseDto result =
                submissionFeedbackService.update("feedback-1", request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("feedback-1");
        assertThat(feedback.getAuthorType()).isEqualTo(FeedbackAuthorType.AI);
        assertThat(feedback.getFeedbackContent()).isEqualTo("Strong feedback content.");

        verify(submissionFeedbackRepository).save(feedback);
    }

    @Test
    void update_whenOnlyFeedbackContentProvided_shouldUpdateFeedbackContentOnlyAndSave() {
        SubmissionFeedback feedback = fullFeedback("feedback-1");
        SubmissionFeedbackUpdateRequestDto request = new SubmissionFeedbackUpdateRequestDto();
        request.setFeedbackContent("Only feedback content changed.");

        when(submissionFeedbackRepository.findById("feedback-1"))
                .thenReturn(Optional.of(feedback));
        when(submissionFeedbackRepository.save(any(SubmissionFeedback.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SubmissionFeedbackResponseDto result =
                submissionFeedbackService.update("feedback-1", request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("feedback-1");
        assertThat(feedback.getAuthorType()).isEqualTo(FeedbackAuthorType.TUTOR);
        assertThat(feedback.getFeedbackContent()).isEqualTo("Only feedback content changed.");

        verify(submissionFeedbackRepository).save(feedback);
    }

    @Test
    void update_whenFeedbackDoesNotExist_shouldThrowRuntimeExceptionAndSkipSave() {
        SubmissionFeedbackUpdateRequestDto request = updateRequest();

        when(submissionFeedbackRepository.findById("missing-feedback"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> submissionFeedbackService.update("missing-feedback", request)
        );

        assertThat(exception.getMessage()).isEqualTo("Submission feedback not found with id: missing-feedback");

        verify(submissionFeedbackRepository).findById("missing-feedback");
        verify(submissionFeedbackRepository, never()).save(any(SubmissionFeedback.class));
    }

    @Test
    void delete_whenFeedbackExists_shouldDeleteFeedback() {
        SubmissionFeedback feedback = fullFeedback("feedback-1");

        when(submissionFeedbackRepository.findById("feedback-1"))
                .thenReturn(Optional.of(feedback));

        submissionFeedbackService.delete("feedback-1");

        verify(submissionFeedbackRepository).findById("feedback-1");
        verify(submissionFeedbackRepository).delete(feedback);
    }

    @Test
    void delete_whenFeedbackDoesNotExist_shouldThrowRuntimeExceptionAndSkipDelete() {
        when(submissionFeedbackRepository.findById("missing-feedback"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> submissionFeedbackService.delete("missing-feedback")
        );

        assertThat(exception.getMessage()).isEqualTo("Submission feedback not found with id: missing-feedback");

        verify(submissionFeedbackRepository).findById("missing-feedback");
        verify(submissionFeedbackRepository, never()).delete(any(SubmissionFeedback.class));
    }

    private SubmissionFeedbackCreateRequestDto createRequest() {
        SubmissionFeedbackCreateRequestDto request = new SubmissionFeedbackCreateRequestDto();

        request.setSubmissionId("submission-1");
        request.setAuthorId("author-1");
        request.setAuthorType(FeedbackAuthorType.TUTOR);
        request.setFeedbackContent("Strong feedback content.");

        return request;
    }

    private SubmissionFeedbackUpdateRequestDto updateRequest() {
        SubmissionFeedbackUpdateRequestDto request = new SubmissionFeedbackUpdateRequestDto();

        request.setAuthorType(FeedbackAuthorType.AI);
        request.setFeedbackContent("Updated feedback content.");

        return request;
    }

    private UserPracticeSubmission submission(String id) {
        UserPracticeSubmission submission = new UserPracticeSubmission();

        submission.setId(id);

        return submission;
    }

    private User author(String userId) {
        User user = new User();

        user.setUserId(userId);
        user.setFirstname("Author");
        user.setLastname("User");
        user.setAvatarUrl("avatar.png");

        return user;
    }

    private SubmissionFeedback fullFeedback(String id) {
        SubmissionFeedback feedback = new SubmissionFeedback();

        feedback.setId(id);
        feedback.setSubmission(submission("submission-1"));
        feedback.setAuthor(author("author-1"));
        feedback.setAuthorType(FeedbackAuthorType.TUTOR);
        feedback.setFeedbackContent("Strong feedback content.");
        feedback.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        feedback.setUpdatedAt(LocalDateTime.of(2026, 1, 2, 10, 0));

        return feedback;
    }

    private void mockIncludes(String... fieldsToInclude) {
        Set<String> includedFields = Set.copyOf(Arrays.asList(fieldsToInclude));

        when(includes.has(anyString())).thenAnswer(invocation -> {
            String fieldName = invocation.getArgument(0);
            return includedFields.contains(fieldName);
        });
    }
}
