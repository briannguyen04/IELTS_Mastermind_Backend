package com.ieltsmastermind.practice.attempt.management.business.service;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.attempt.management.business.interfaces.UserPracticeSubmissionService;
import com.ieltsmastermind.practice.attempt.management.domain.dto.TutorUserPracticeSubmissionResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.TutorUserPracticeSubmissionSetStatusRequestDto;
import com.ieltsmastermind.practice.attempt.management.domain.entity.TutorUserPracticeSubmission;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeSubmission;
import com.ieltsmastermind.practice.attempt.management.domain.enums.TutorStatus;
import com.ieltsmastermind.practice.attempt.management.persistence.TutorUserPracticeSubmissionRepository;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeSubmissionRepository;
import com.ieltsmastermind.user.management.domain.entity.User;
import com.ieltsmastermind.user.management.persistence.UserRepository;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TutorUserPracticeSubmissionServiceImplTest {

    @Mock
    private TutorUserPracticeSubmissionRepository tutorUserPracticeSubmissionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPracticeSubmissionRepository userPracticeSubmissionRepository;

    @Mock
    private UserPracticeSubmissionService userPracticeSubmissionService;

    @Mock
    private IncludeSpec includes;

    @InjectMocks
    private TutorUserPracticeSubmissionServiceImpl tutorUserPracticeSubmissionService;

    @Test
    void setTutorStatus_whenTutorAndSubmissionExistAndNoRowExistsAndStatusIsPending_shouldCreateRowSaveSyncAndReturnId() {
        String tutorId = "tutor-1";
        String submissionId = "submission-1";
        User tutor = tutor(tutorId);
        UserPracticeSubmission submission = submission(submissionId);
        TutorUserPracticeSubmissionSetStatusRequestDto request = setStatusRequest(TutorStatus.PENDING);

        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(userPracticeSubmissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        when(tutorUserPracticeSubmissionRepository.findByTutor_UserIdAndUserPracticeSubmission_Id(tutorId, submissionId))
                .thenReturn(Optional.empty());
        when(tutorUserPracticeSubmissionRepository.save(any(TutorUserPracticeSubmission.class)))
                .thenAnswer(invocation -> {
                    TutorUserPracticeSubmission row = invocation.getArgument(0);
                    row.setId("tutor-submission-1");
                    return row;
                });

        TutorUserPracticeSubmissionResponseDto result =
                tutorUserPracticeSubmissionService.setTutorStatus(tutorId, submissionId, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("tutor-submission-1");

        ArgumentCaptor<TutorUserPracticeSubmission> rowCaptor =
                ArgumentCaptor.forClass(TutorUserPracticeSubmission.class);
        verify(tutorUserPracticeSubmissionRepository).save(rowCaptor.capture());

        TutorUserPracticeSubmission savedRow = rowCaptor.getValue();

        assertThat(savedRow.getTutor()).isSameAs(tutor);
        assertThat(savedRow.getUserPracticeSubmission()).isSameAs(submission);
        assertThat(savedRow.getTutorStatus()).isEqualTo(TutorStatus.PENDING);

        verify(userRepository).findById(tutorId);
        verify(userPracticeSubmissionRepository).findById(submissionId);
        verify(tutorUserPracticeSubmissionRepository, never())
                .existsByUserPracticeSubmission_IdAndTutor_UserIdNotAndTutorStatusIn(
                        anyString(),
                        anyString(),
                        anyCollection()
                );
        verify(userPracticeSubmissionService).syncTutorStatusFromTutorSubmissions(submissionId);
    }

    @Test
    void setTutorStatus_whenExistingRowAndStatusIsInReviewAndNoOtherTutorIsActive_shouldUpdateRowSaveSyncAndReturnId() {
        String tutorId = "tutor-1";
        String submissionId = "submission-1";
        User tutor = tutor(tutorId);
        UserPracticeSubmission submission = submission(submissionId);
        TutorUserPracticeSubmission existingRow = row("tutor-submission-1", tutor, submission, TutorStatus.PENDING);
        TutorUserPracticeSubmissionSetStatusRequestDto request = setStatusRequest(TutorStatus.IN_REVIEW);

        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(userPracticeSubmissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        when(tutorUserPracticeSubmissionRepository.existsByUserPracticeSubmission_IdAndTutor_UserIdNotAndTutorStatusIn(
                eq(submissionId),
                eq(tutorId),
                anyCollection()
        )).thenReturn(false);
        when(tutorUserPracticeSubmissionRepository.findByTutor_UserIdAndUserPracticeSubmission_Id(tutorId, submissionId))
                .thenReturn(Optional.of(existingRow));
        when(tutorUserPracticeSubmissionRepository.save(any(TutorUserPracticeSubmission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TutorUserPracticeSubmissionResponseDto result =
                tutorUserPracticeSubmissionService.setTutorStatus(tutorId, submissionId, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("tutor-submission-1");
        assertThat(existingRow.getTutorStatus()).isEqualTo(TutorStatus.IN_REVIEW);

        verify(tutorUserPracticeSubmissionRepository).existsByUserPracticeSubmission_IdAndTutor_UserIdNotAndTutorStatusIn(
                eq(submissionId),
                eq(tutorId),
                anyCollection()
        );
        verify(tutorUserPracticeSubmissionRepository).save(existingRow);
        verify(userPracticeSubmissionService).syncTutorStatusFromTutorSubmissions(submissionId);
    }

    @Test
    void setTutorStatus_whenStatusIsCompletedAndNoExistingRow_shouldCreateCompletedRowSaveSyncAndReturnId() {
        String tutorId = "tutor-1";
        String submissionId = "submission-1";
        User tutor = tutor(tutorId);
        UserPracticeSubmission submission = submission(submissionId);
        TutorUserPracticeSubmissionSetStatusRequestDto request = setStatusRequest(TutorStatus.COMPLETED);

        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(userPracticeSubmissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        when(tutorUserPracticeSubmissionRepository.existsByUserPracticeSubmission_IdAndTutor_UserIdNotAndTutorStatusIn(
                eq(submissionId),
                eq(tutorId),
                anyCollection()
        )).thenReturn(false);
        when(tutorUserPracticeSubmissionRepository.findByTutor_UserIdAndUserPracticeSubmission_Id(tutorId, submissionId))
                .thenReturn(Optional.empty());
        when(tutorUserPracticeSubmissionRepository.save(any(TutorUserPracticeSubmission.class)))
                .thenAnswer(invocation -> {
                    TutorUserPracticeSubmission row = invocation.getArgument(0);
                    row.setId("tutor-submission-1");
                    return row;
                });

        TutorUserPracticeSubmissionResponseDto result =
                tutorUserPracticeSubmissionService.setTutorStatus(tutorId, submissionId, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("tutor-submission-1");

        ArgumentCaptor<TutorUserPracticeSubmission> rowCaptor =
                ArgumentCaptor.forClass(TutorUserPracticeSubmission.class);
        verify(tutorUserPracticeSubmissionRepository).save(rowCaptor.capture());

        assertThat(rowCaptor.getValue().getTutor()).isSameAs(tutor);
        assertThat(rowCaptor.getValue().getUserPracticeSubmission()).isSameAs(submission);
        assertThat(rowCaptor.getValue().getTutorStatus()).isEqualTo(TutorStatus.COMPLETED);

        verify(userPracticeSubmissionService).syncTutorStatusFromTutorSubmissions(submissionId);
    }

    @Test
    void setTutorStatus_whenStatusIsCompletedAndAnotherTutorIsActive_shouldThrowRuntimeExceptionAndSkipSaveSync() {
        String tutorId = "tutor-1";
        String submissionId = "submission-1";
        User tutor = tutor(tutorId);
        UserPracticeSubmission submission = submission(submissionId);
        TutorUserPracticeSubmissionSetStatusRequestDto request = setStatusRequest(TutorStatus.COMPLETED);

        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(userPracticeSubmissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        when(tutorUserPracticeSubmissionRepository.existsByUserPracticeSubmission_IdAndTutor_UserIdNotAndTutorStatusIn(
                eq(submissionId),
                eq(tutorId),
                anyCollection()
        )).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> tutorUserPracticeSubmissionService.setTutorStatus(tutorId, submissionId, request)
        );

        assertThat(exception.getMessage())
                .isEqualTo("This submission is already being reviewed or has been completed by another tutor.");

        verify(tutorUserPracticeSubmissionRepository).existsByUserPracticeSubmission_IdAndTutor_UserIdNotAndTutorStatusIn(
                eq(submissionId),
                eq(tutorId),
                anyCollection()
        );
        verify(tutorUserPracticeSubmissionRepository, never())
                .findByTutor_UserIdAndUserPracticeSubmission_Id(anyString(), anyString());
        verify(tutorUserPracticeSubmissionRepository, never()).save(any(TutorUserPracticeSubmission.class));
        verify(userPracticeSubmissionService, never()).syncTutorStatusFromTutorSubmissions(anyString());
    }

    @Test
    void setTutorStatus_whenTutorDoesNotExist_shouldThrowRuntimeExceptionAndSkipSubmissionLookup() {
        String tutorId = "missing-tutor";
        String submissionId = "submission-1";
        TutorUserPracticeSubmissionSetStatusRequestDto request = setStatusRequest(TutorStatus.IN_REVIEW);

        when(userRepository.findById(tutorId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> tutorUserPracticeSubmissionService.setTutorStatus(tutorId, submissionId, request)
        );

        assertThat(exception.getMessage()).isEqualTo("Tutor not found with id: missing-tutor");

        verify(userRepository).findById(tutorId);
        verify(userPracticeSubmissionRepository, never()).findById(anyString());
        verify(tutorUserPracticeSubmissionRepository, never()).save(any(TutorUserPracticeSubmission.class));
        verify(userPracticeSubmissionService, never()).syncTutorStatusFromTutorSubmissions(anyString());
    }

    @Test
    void setTutorStatus_whenSubmissionDoesNotExist_shouldThrowRuntimeExceptionAndSkipValidationAndSave() {
        String tutorId = "tutor-1";
        String submissionId = "missing-submission";
        User tutor = tutor(tutorId);
        TutorUserPracticeSubmissionSetStatusRequestDto request = setStatusRequest(TutorStatus.IN_REVIEW);

        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(userPracticeSubmissionRepository.findById(submissionId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> tutorUserPracticeSubmissionService.setTutorStatus(tutorId, submissionId, request)
        );

        assertThat(exception.getMessage())
                .isEqualTo("User practice submission not found with id: missing-submission");

        verify(userRepository).findById(tutorId);
        verify(userPracticeSubmissionRepository).findById(submissionId);
        verify(tutorUserPracticeSubmissionRepository, never())
                .existsByUserPracticeSubmission_IdAndTutor_UserIdNotAndTutorStatusIn(
                        anyString(),
                        anyString(),
                        anyCollection()
                );
        verify(tutorUserPracticeSubmissionRepository, never()).save(any(TutorUserPracticeSubmission.class));
        verify(userPracticeSubmissionService, never()).syncTutorStatusFromTutorSubmissions(anyString());
    }

    @Test
    void setTutorStatus_whenRequestedStatusIsNull_shouldThrowNullPointerExceptionAndSkipSaveSync() {
        String tutorId = "tutor-1";
        String submissionId = "submission-1";
        User tutor = tutor(tutorId);
        UserPracticeSubmission submission = submission(submissionId);
        TutorUserPracticeSubmissionSetStatusRequestDto request = setStatusRequest(null);

        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(userPracticeSubmissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> tutorUserPracticeSubmissionService.setTutorStatus(tutorId, submissionId, request)
        );

        assertThat(exception).isNotNull();

        verify(userRepository).findById(tutorId);
        verify(userPracticeSubmissionRepository).findById(submissionId);
        verify(tutorUserPracticeSubmissionRepository, never())
                .existsByUserPracticeSubmission_IdAndTutor_UserIdNotAndTutorStatusIn(
                        anyString(),
                        anyString(),
                        anyCollection()
                );
        verify(tutorUserPracticeSubmissionRepository, never())
                .findByTutor_UserIdAndUserPracticeSubmission_Id(anyString(), anyString());
        verify(tutorUserPracticeSubmissionRepository, never()).save(any(TutorUserPracticeSubmission.class));
        verify(userPracticeSubmissionService, never()).syncTutorStatusFromTutorSubmissions(anyString());
    }

    @Test
    void getAllByTutorId_whenRowsExistAndFieldsAreIncluded_shouldReturnDtosWithIncludedFields() {
        String tutorId = "tutor-1";
        User tutor = tutor(tutorId);
        UserPracticeSubmission submission1 = submission("submission-1");
        UserPracticeSubmission submission2 = submission("submission-2");
        TutorUserPracticeSubmission row1 = row("tutor-submission-1", tutor, submission1, TutorStatus.IN_REVIEW);
        TutorUserPracticeSubmission row2 = row("tutor-submission-2", tutor, submission2, TutorStatus.COMPLETED);

        when(tutorUserPracticeSubmissionRepository.findAllByTutor_UserId(tutorId))
                .thenReturn(List.of(row1, row2));
        mockIncludes("tutorid", "userpracticesubmissionid", "tutorstatus");

        List<TutorUserPracticeSubmissionResponseDto> result =
                tutorUserPracticeSubmissionService.getAllByTutorId(tutorId, includes);

        assertThat(result).hasSize(2);

        assertThat(result.get(0).getId()).isEqualTo("tutor-submission-1");
        assertThat(result.get(0).getTutorId()).isEqualTo(tutorId);
        assertThat(result.get(0).getUserPracticeSubmissionId()).isEqualTo("submission-1");
        assertThat(result.get(0).getTutorStatus()).isEqualTo(TutorStatus.IN_REVIEW);

        assertThat(result.get(1).getId()).isEqualTo("tutor-submission-2");
        assertThat(result.get(1).getTutorId()).isEqualTo(tutorId);
        assertThat(result.get(1).getUserPracticeSubmissionId()).isEqualTo("submission-2");
        assertThat(result.get(1).getTutorStatus()).isEqualTo(TutorStatus.COMPLETED);

        verify(tutorUserPracticeSubmissionRepository).findAllByTutor_UserId(tutorId);
    }

    @Test
    void getAllByTutorId_whenNoFieldsAreIncluded_shouldReturnOnlyIds() {
        String tutorId = "tutor-1";
        User tutor = tutor(tutorId);
        UserPracticeSubmission submission = submission("submission-1");
        TutorUserPracticeSubmission row = row("tutor-submission-1", tutor, submission, TutorStatus.IN_REVIEW);

        when(tutorUserPracticeSubmissionRepository.findAllByTutor_UserId(tutorId))
                .thenReturn(List.of(row));
        mockIncludes();

        List<TutorUserPracticeSubmissionResponseDto> result =
                tutorUserPracticeSubmissionService.getAllByTutorId(tutorId, includes);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("tutor-submission-1");
        assertThat(result.get(0).getTutorId()).isNull();
        assertThat(result.get(0).getUserPracticeSubmissionId()).isNull();
        assertThat(result.get(0).getTutorStatus()).isNull();

        verify(tutorUserPracticeSubmissionRepository).findAllByTutor_UserId(tutorId);
    }

    @Test
    void getAllByTutorId_whenRepositoryReturnsEmptyList_shouldReturnEmptyList() {
        String tutorId = "tutor-1";

        when(tutorUserPracticeSubmissionRepository.findAllByTutor_UserId(tutorId))
                .thenReturn(List.of());

        List<TutorUserPracticeSubmissionResponseDto> result =
                tutorUserPracticeSubmissionService.getAllByTutorId(tutorId, includes);

        assertThat(result).isEmpty();

        verify(tutorUserPracticeSubmissionRepository).findAllByTutor_UserId(tutorId);
    }

    @Test
    void getByTutorIdAndUserPracticeSubmissionId_whenRowExistsAndFieldsAreIncluded_shouldReturnDtoWithIncludedFields() {
        String tutorId = "tutor-1";
        String submissionId = "submission-1";
        User tutor = tutor(tutorId);
        UserPracticeSubmission submission = submission(submissionId);
        TutorUserPracticeSubmission row = row("tutor-submission-1", tutor, submission, TutorStatus.IN_REVIEW);

        when(tutorUserPracticeSubmissionRepository.findByTutor_UserIdAndUserPracticeSubmission_Id(tutorId, submissionId))
                .thenReturn(Optional.of(row));
        mockIncludes("tutorid", "userpracticesubmissionid", "tutorstatus");

        TutorUserPracticeSubmissionResponseDto result =
                tutorUserPracticeSubmissionService.getByTutorIdAndUserPracticeSubmissionId(
                        tutorId,
                        submissionId,
                        includes
                );

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("tutor-submission-1");
        assertThat(result.getTutorId()).isEqualTo(tutorId);
        assertThat(result.getUserPracticeSubmissionId()).isEqualTo(submissionId);
        assertThat(result.getTutorStatus()).isEqualTo(TutorStatus.IN_REVIEW);

        verify(tutorUserPracticeSubmissionRepository)
                .findByTutor_UserIdAndUserPracticeSubmission_Id(tutorId, submissionId);
    }

    @Test
    void getByTutorIdAndUserPracticeSubmissionId_whenRowExistsAndNoFieldsAreIncluded_shouldReturnOnlyId() {
        String tutorId = "tutor-1";
        String submissionId = "submission-1";
        User tutor = tutor(tutorId);
        UserPracticeSubmission submission = submission(submissionId);
        TutorUserPracticeSubmission row = row("tutor-submission-1", tutor, submission, TutorStatus.IN_REVIEW);

        when(tutorUserPracticeSubmissionRepository.findByTutor_UserIdAndUserPracticeSubmission_Id(tutorId, submissionId))
                .thenReturn(Optional.of(row));
        mockIncludes();

        TutorUserPracticeSubmissionResponseDto result =
                tutorUserPracticeSubmissionService.getByTutorIdAndUserPracticeSubmissionId(
                        tutorId,
                        submissionId,
                        includes
                );

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("tutor-submission-1");
        assertThat(result.getTutorId()).isNull();
        assertThat(result.getUserPracticeSubmissionId()).isNull();
        assertThat(result.getTutorStatus()).isNull();

        verify(tutorUserPracticeSubmissionRepository)
                .findByTutor_UserIdAndUserPracticeSubmission_Id(tutorId, submissionId);
    }

    @Test
    void getByTutorIdAndUserPracticeSubmissionId_whenRowDoesNotExist_shouldReturnTutorAndSubmissionIdsOnly() {
        String tutorId = "tutor-1";
        String submissionId = "submission-1";

        when(tutorUserPracticeSubmissionRepository.findByTutor_UserIdAndUserPracticeSubmission_Id(tutorId, submissionId))
                .thenReturn(Optional.empty());

        TutorUserPracticeSubmissionResponseDto result =
                tutorUserPracticeSubmissionService.getByTutorIdAndUserPracticeSubmissionId(
                        tutorId,
                        submissionId,
                        includes
                );

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNull();
        assertThat(result.getTutorId()).isEqualTo(tutorId);
        assertThat(result.getUserPracticeSubmissionId()).isEqualTo(submissionId);
        assertThat(result.getTutorStatus()).isNull();

        verify(tutorUserPracticeSubmissionRepository)
                .findByTutor_UserIdAndUserPracticeSubmission_Id(tutorId, submissionId);
        verify(includes, never()).has(anyString());
    }

    private TutorUserPracticeSubmissionSetStatusRequestDto setStatusRequest(TutorStatus tutorStatus) {
        TutorUserPracticeSubmissionSetStatusRequestDto request =
                new TutorUserPracticeSubmissionSetStatusRequestDto();

        request.setTutorStatus(tutorStatus);

        return request;
    }

    private User tutor(String tutorId) {
        User tutor = new User();

        tutor.setUserId(tutorId);
        tutor.setEmail(tutorId + "@example.com");
        tutor.setFirstname("Tutor");
        tutor.setLastname("User");

        return tutor;
    }

    private UserPracticeSubmission submission(String submissionId) {
        UserPracticeSubmission submission = new UserPracticeSubmission();

        submission.setId(submissionId);

        return submission;
    }

    private TutorUserPracticeSubmission row(String id,
                                            User tutor,
                                            UserPracticeSubmission submission,
                                            TutorStatus tutorStatus) {
        TutorUserPracticeSubmission row = new TutorUserPracticeSubmission();

        row.setId(id);
        row.setTutor(tutor);
        row.setUserPracticeSubmission(submission);
        row.setTutorStatus(tutorStatus);

        return row;
    }

    private void mockIncludes(String... fieldsToInclude) {
        Set<String> includedFields = Set.copyOf(Arrays.asList(fieldsToInclude));

        when(includes.has(anyString())).thenAnswer(invocation -> {
            String fieldName = invocation.getArgument(0);
            return includedFields.contains(fieldName);
        });
    }
}
