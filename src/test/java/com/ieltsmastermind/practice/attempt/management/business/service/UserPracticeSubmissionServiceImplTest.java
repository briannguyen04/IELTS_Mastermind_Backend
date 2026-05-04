package com.ieltsmastermind.practice.attempt.management.business.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieltsmastermind.common.json.JsonConverter;
import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.attempt.management.domain.dto.LearnerTestActivityCreateRequestDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeSubmissionCreateRequestDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeSubmissionResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeSubmissionUpdateRequestDto;
import com.ieltsmastermind.practice.attempt.management.domain.entity.SubmissionQuestionTypeAccuracy;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeSubmission;
import com.ieltsmastermind.practice.attempt.management.domain.enums.LearnerTestActivityType;
import com.ieltsmastermind.practice.attempt.management.domain.enums.TutorStatus;
import com.ieltsmastermind.practice.attempt.management.persistence.TutorUserPracticeSubmissionRepository;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeSubmissionRepository;
import com.ieltsmastermind.practice.content.management.domain.entity.PracticeContent;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTaskType;
import com.ieltsmastermind.practice.content.management.persistence.PracticeContentRepository;
import com.ieltsmastermind.user.management.domain.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
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
class UserPracticeSubmissionServiceImplTest {

    @Mock
    private UserPracticeSubmissionRepository userPracticeSubmissionRepository;

    @Mock
    private PracticeContentRepository practiceContentRepository;

    @Mock
    private TutorUserPracticeSubmissionRepository tutorUserPracticeSubmissionRepository;

    @Mock
    private JsonConverter jsonConverter;

    @Mock
    private IncludeSpec includes;

    @Mock
    private JsonNode learnerActivitiesJson;

    @InjectMocks
    private UserPracticeSubmissionServiceImpl userPracticeSubmissionService;

    @Test
    void create_whenRequestIsValid_shouldSaveSubmissionIncrementAttemptCountAndReturnSubmissionId() {
        UserPracticeSubmissionCreateRequestDto request = createRequest();

        when(jsonConverter.toJsonNode(anyList())).thenReturn(learnerActivitiesJson);
        when(userPracticeSubmissionRepository.save(any(UserPracticeSubmission.class))).thenAnswer(invocation -> {
            UserPracticeSubmission submission = invocation.getArgument(0);
            submission.setId("submission-1");
            return submission;
        });
        when(practiceContentRepository.incrementAttemptCount("content-1")).thenReturn(1);

        UserPracticeSubmissionResponseDto result = userPracticeSubmissionService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("submission-1");

        ArgumentCaptor<UserPracticeSubmission> submissionCaptor =
                ArgumentCaptor.forClass(UserPracticeSubmission.class);
        verify(userPracticeSubmissionRepository).save(submissionCaptor.capture());

        UserPracticeSubmission savedSubmission = submissionCaptor.getValue();

        assertThat(savedSubmission.getUserId()).isEqualTo("user-1");
        assertThat(savedSubmission.getPracticeContentId()).isEqualTo("content-1");
        assertThat(savedSubmission.getTimeSpentSeconds()).isEqualTo(120);
        assertThat(savedSubmission.getSubmittedAt()).isNotNull();
        assertThat(savedSubmission.getLearnerTestActivities()).isSameAs(learnerActivitiesJson);

        verify(jsonConverter).toJsonNode(request.getLearnerTestActivities());
        verify(practiceContentRepository).incrementAttemptCount("content-1");
    }

    @Test
    void create_whenPracticeContentAttemptCountIsNotUpdated_shouldThrowIllegalArgumentExceptionAfterSave() {
        UserPracticeSubmissionCreateRequestDto request = createRequest();
        request.setPracticeContentId("missing-content");

        when(jsonConverter.toJsonNode(anyList())).thenReturn(learnerActivitiesJson);
        when(userPracticeSubmissionRepository.save(any(UserPracticeSubmission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(practiceContentRepository.incrementAttemptCount("missing-content")).thenReturn(0);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userPracticeSubmissionService.create(request)
        );

        assertThat(exception.getMessage()).isEqualTo("PracticeContent not found: missing-content");

        verify(userPracticeSubmissionRepository).save(any(UserPracticeSubmission.class));
        verify(practiceContentRepository).incrementAttemptCount("missing-content");
    }

    @Test
    void getAllByUserId_whenSubmissionsExistAndFieldsAreIncluded_shouldReturnDtosWithIncludedFields() {
        String userId = "user-1";
        UserPracticeSubmission submission = fullSubmission("submission-1");

        when(userPracticeSubmissionRepository.findAllByUserIdOrderBySubmittedAtDesc(userId))
                .thenReturn(List.of(submission));
        mockIncludes(
                "userid",
                "practicecontentid",
                "timespentseconds",
                "submittedat",
                "score",
                "correctanswerpercentage",
                "correctanswercount",
                "wronganswercount",
                "skipanswercount",
                "istutorreviewrequested",
                "tutorstatus",
                "user.userid",
                "user.email",
                "user.firstname",
                "user.lastname",
                "practicecontent.id",
                "practicecontent.skill",
                "practicecontent.task",
                "practicecontent.title",
                "questiontypeaccuracies.questiontype",
                "questiontypeaccuracies.correctanswerpercentage"
        );

        List<UserPracticeSubmissionResponseDto> result =
                userPracticeSubmissionService.getAllByUserId(userId, includes);

        assertThat(result).hasSize(1);

        UserPracticeSubmissionResponseDto dto = result.get(0);

        assertThat(dto.getId()).isEqualTo("submission-1");
        assertThat(dto.getUserId()).isEqualTo("user-1");
        assertThat(dto.getPracticeContentId()).isEqualTo("content-1");
        assertThat(dto.getTimeSpentSeconds()).isEqualTo(120);
        assertThat(dto.getSubmittedAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 0));
        assertThat(dto.getScore()).isEqualTo(8.0);
        assertThat(dto.getCorrectAnswerPercentage()).isEqualTo(75.0);
        assertThat(dto.getCorrectAnswerCount()).isEqualTo(15);
        assertThat(dto.getWrongAnswerCount()).isEqualTo(4);
        assertThat(dto.getSkipAnswerCount()).isEqualTo(1);
        assertThat(dto.getIsTutorReviewRequested()).isTrue();
        assertThat(dto.getTutorStatus()).isEqualTo(TutorStatus.IN_REVIEW);

        assertThat(dto.getUser()).isNotNull();
        assertThat(dto.getUser().getUserId()).isEqualTo("user-1");
        assertThat(dto.getUser().getEmail()).isEqualTo("john@example.com");
        assertThat(dto.getUser().getFirstname()).isEqualTo("John");
        assertThat(dto.getUser().getLastname()).isEqualTo("Doe");

        assertThat(dto.getPracticeContent()).isNotNull();
        assertThat(dto.getPracticeContent().getId()).isEqualTo("content-1");
        assertThat(dto.getPracticeContent().getSkill()).isEqualTo(PracticeContentSkill.LISTENING);
        assertThat(dto.getPracticeContent().getTask()).isEqualTo(PracticeTaskType.TASK_1);
        assertThat(dto.getPracticeContent().getTitle()).isEqualTo("Listening Practice");

        assertThat(dto.getQuestionTypeAccuracies()).hasSize(1);
        assertThat(dto.getQuestionTypeAccuracies().get(0).getQuestionType())
                .isEqualTo(PracticeQuestionType.MULTIPLE_CHOICE);
        assertThat(dto.getQuestionTypeAccuracies().get(0).getCorrectAnswerPercentage())
                .isEqualTo(75.0);

        verify(userPracticeSubmissionRepository).findAllByUserIdOrderBySubmittedAtDesc(userId);
    }

    @Test
    void getAllByUserId_whenNoFieldsAreIncluded_shouldReturnOnlySubmissionId() {
        String userId = "user-1";
        UserPracticeSubmission submission = fullSubmission("submission-1");

        when(userPracticeSubmissionRepository.findAllByUserIdOrderBySubmittedAtDesc(userId))
                .thenReturn(List.of(submission));
        mockIncludes();

        List<UserPracticeSubmissionResponseDto> result =
                userPracticeSubmissionService.getAllByUserId(userId, includes);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("submission-1");
        assertThat(result.get(0).getUserId()).isNull();
        assertThat(result.get(0).getPracticeContentId()).isNull();
        assertThat(result.get(0).getUser()).isNull();
        assertThat(result.get(0).getPracticeContent()).isNull();
        assertThat(result.get(0).getQuestionTypeAccuracies()).isNull();

        verify(userPracticeSubmissionRepository).findAllByUserIdOrderBySubmittedAtDesc(userId);
    }

    @Test
    void getAllByUserId_whenNestedUserIncludeRequestedButUserIsNull_shouldNotSetUserDto() {
        String userId = "user-1";
        UserPracticeSubmission submission = fullSubmission("submission-1");
        submission.setUser(null);

        when(userPracticeSubmissionRepository.findAllByUserIdOrderBySubmittedAtDesc(userId))
                .thenReturn(List.of(submission));
        mockIncludes("user.userid", "user.email");

        List<UserPracticeSubmissionResponseDto> result =
                userPracticeSubmissionService.getAllByUserId(userId, includes);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("submission-1");
        assertThat(result.get(0).getUser()).isNull();

        verify(userPracticeSubmissionRepository).findAllByUserIdOrderBySubmittedAtDesc(userId);
    }

    @Test
    void getAllByUserId_whenNestedPracticeContentIncludeRequestedButPracticeContentIsNull_shouldNotSetPracticeContentDto() {
        String userId = "user-1";
        UserPracticeSubmission submission = fullSubmission("submission-1");
        submission.setPracticeContent(null);

        when(userPracticeSubmissionRepository.findAllByUserIdOrderBySubmittedAtDesc(userId))
                .thenReturn(List.of(submission));
        mockIncludes("practicecontent.id", "practicecontent.skill");

        List<UserPracticeSubmissionResponseDto> result =
                userPracticeSubmissionService.getAllByUserId(userId, includes);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("submission-1");
        assertThat(result.get(0).getPracticeContent()).isNull();

        verify(userPracticeSubmissionRepository).findAllByUserIdOrderBySubmittedAtDesc(userId);
    }

    @Test
    void getAllByUserId_whenQuestionTypeAccuraciesAreNull_shouldNotSetQuestionTypeAccuraciesDto() {
        String userId = "user-1";
        UserPracticeSubmission submission = fullSubmission("submission-1");
        submission.setQuestionTypeAccuracies(null);

        when(userPracticeSubmissionRepository.findAllByUserIdOrderBySubmittedAtDesc(userId))
                .thenReturn(List.of(submission));
        mockIncludes(
                "questiontypeaccuracies.questiontype",
                "questiontypeaccuracies.correctanswerpercentage"
        );

        List<UserPracticeSubmissionResponseDto> result =
                userPracticeSubmissionService.getAllByUserId(userId, includes);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("submission-1");
        assertThat(result.get(0).getQuestionTypeAccuracies()).isNull();

        verify(userPracticeSubmissionRepository).findAllByUserIdOrderBySubmittedAtDesc(userId);
    }

    @Test
    void getAllByUserId_whenRepositoryReturnsEmptyList_shouldReturnEmptyList() {
        String userId = "user-1";

        when(userPracticeSubmissionRepository.findAllByUserIdOrderBySubmittedAtDesc(userId))
                .thenReturn(List.of());

        List<UserPracticeSubmissionResponseDto> result =
                userPracticeSubmissionService.getAllByUserId(userId, includes);

        assertThat(result).isEmpty();

        verify(userPracticeSubmissionRepository).findAllByUserIdOrderBySubmittedAtDesc(userId);
    }

    @Test
    void getById_whenSubmissionExists_shouldReturnSubmissionWithIncludedFields() {
        UserPracticeSubmission submission = fullSubmission("submission-1");

        when(userPracticeSubmissionRepository.findById("submission-1"))
                .thenReturn(Optional.of(submission));
        mockIncludes("userid", "practicecontentid", "score");

        UserPracticeSubmissionResponseDto result =
                userPracticeSubmissionService.getById("submission-1", includes);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("submission-1");
        assertThat(result.getUserId()).isEqualTo("user-1");
        assertThat(result.getPracticeContentId()).isEqualTo("content-1");
        assertThat(result.getScore()).isEqualTo(8.0);
        assertThat(result.getTimeSpentSeconds()).isNull();

        verify(userPracticeSubmissionRepository).findById("submission-1");
    }

    @Test
    void getById_whenSubmissionDoesNotExist_shouldThrowRuntimeException() {
        when(userPracticeSubmissionRepository.findById("missing-submission"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userPracticeSubmissionService.getById("missing-submission", includes)
        );

        assertThat(exception.getMessage()).isEqualTo("Submission not found");

        verify(userPracticeSubmissionRepository).findById("missing-submission");
    }

    @Test
    void getAll_whenSubmissionsExist_shouldReturnDtosOrderedByRepositoryResult() {
        UserPracticeSubmission submission1 = fullSubmission("submission-1");
        UserPracticeSubmission submission2 = fullSubmission("submission-2");
        submission2.setScore(6.5);

        when(userPracticeSubmissionRepository.findAllByOrderBySubmittedAtDesc())
                .thenReturn(List.of(submission1, submission2));
        mockIncludes("score");

        List<UserPracticeSubmissionResponseDto> result =
                userPracticeSubmissionService.getAll(includes);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("submission-1");
        assertThat(result.get(0).getScore()).isEqualTo(8.0);
        assertThat(result.get(1).getId()).isEqualTo("submission-2");
        assertThat(result.get(1).getScore()).isEqualTo(6.5);

        verify(userPracticeSubmissionRepository).findAllByOrderBySubmittedAtDesc();
    }

    @Test
    void getAll_whenRepositoryReturnsEmptyList_shouldReturnEmptyList() {
        when(userPracticeSubmissionRepository.findAllByOrderBySubmittedAtDesc())
                .thenReturn(List.of());

        List<UserPracticeSubmissionResponseDto> result =
                userPracticeSubmissionService.getAll(includes);

        assertThat(result).isEmpty();

        verify(userPracticeSubmissionRepository).findAllByOrderBySubmittedAtDesc();
    }

    @Test
    void getAllTutorReviewRequested_whenSubmissionsExist_shouldReturnDtos() {
        UserPracticeSubmission submission = fullSubmission("submission-1");

        when(userPracticeSubmissionRepository.findByIsTutorReviewRequestedTrueOrderBySubmittedAtDesc())
                .thenReturn(List.of(submission));
        mockIncludes("istutorreviewrequested", "tutorstatus");

        List<UserPracticeSubmissionResponseDto> result =
                userPracticeSubmissionService.getAllTutorReviewRequested(includes);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("submission-1");
        assertThat(result.get(0).getIsTutorReviewRequested()).isTrue();
        assertThat(result.get(0).getTutorStatus()).isEqualTo(TutorStatus.IN_REVIEW);

        verify(userPracticeSubmissionRepository).findByIsTutorReviewRequestedTrueOrderBySubmittedAtDesc();
    }

    @Test
    void getAllTutorReviewRequested_whenRepositoryReturnsEmptyList_shouldReturnEmptyList() {
        when(userPracticeSubmissionRepository.findByIsTutorReviewRequestedTrueOrderBySubmittedAtDesc())
                .thenReturn(List.of());

        List<UserPracticeSubmissionResponseDto> result =
                userPracticeSubmissionService.getAllTutorReviewRequested(includes);

        assertThat(result).isEmpty();

        verify(userPracticeSubmissionRepository).findByIsTutorReviewRequestedTrueOrderBySubmittedAtDesc();
    }

    @Test
    void getAllByUserIdAndDays_whenDaysIsPositive_shouldSearchBetweenDateRangeAndReturnDtos() {
        String userId = "user-1";
        UserPracticeSubmission submission = fullSubmission("submission-1");

        when(userPracticeSubmissionRepository.findAllByUserIdAndSubmittedAtBetweenOrderBySubmittedAtDesc(
                anyString(),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of(submission));
        mockIncludes("userid", "submittedat");

        List<UserPracticeSubmissionResponseDto> result =
                userPracticeSubmissionService.getAllByUserIdAndDays(userId, 7, includes);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("submission-1");
        assertThat(result.get(0).getUserId()).isEqualTo("user-1");
        assertThat(result.get(0).getSubmittedAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 0));

        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(userPracticeSubmissionRepository).findAllByUserIdAndSubmittedAtBetweenOrderBySubmittedAtDesc(
                anyString(),
                startCaptor.capture(),
                endCaptor.capture()
        );

        assertThat(startCaptor.getValue()).isBefore(endCaptor.getValue());
    }

    @Test
    void getAllByUserIdAndDays_whenRepositoryReturnsEmptyList_shouldReturnEmptyList() {
        String userId = "user-1";

        when(userPracticeSubmissionRepository.findAllByUserIdAndSubmittedAtBetweenOrderBySubmittedAtDesc(
                anyString(),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of());

        List<UserPracticeSubmissionResponseDto> result =
                userPracticeSubmissionService.getAllByUserIdAndDays(userId, 7, includes);

        assertThat(result).isEmpty();

        verify(userPracticeSubmissionRepository).findAllByUserIdAndSubmittedAtBetweenOrderBySubmittedAtDesc(
                anyString(),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
    }

    @Test
    void getAllByUserIdAndDays_whenDaysIsZero_shouldThrowRuntimeExceptionAndSkipRepositoryLookup() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userPracticeSubmissionService.getAllByUserIdAndDays("user-1", 0, includes)
        );

        assertThat(exception.getMessage()).isEqualTo("Days must be greater than 0");

        verify(userPracticeSubmissionRepository, never())
                .findAllByUserIdAndSubmittedAtBetweenOrderBySubmittedAtDesc(
                        anyString(),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class)
                );
    }

    @Test
    void getAllByUserIdAndDays_whenDaysIsNegative_shouldThrowRuntimeExceptionAndSkipRepositoryLookup() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userPracticeSubmissionService.getAllByUserIdAndDays("user-1", -1, includes)
        );

        assertThat(exception.getMessage()).isEqualTo("Days must be greater than 0");

        verify(userPracticeSubmissionRepository, never())
                .findAllByUserIdAndSubmittedAtBetweenOrderBySubmittedAtDesc(
                        anyString(),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class)
                );
    }

    @Test
    void update_whenSubmissionExistsAndTutorReviewRequestedProvided_shouldUpdateFlagSaveAndReturnSubmissionId() {
        UserPracticeSubmission submission = fullSubmission("submission-1");
        submission.setTutorReviewRequested(false);

        UserPracticeSubmissionUpdateRequestDto request = new UserPracticeSubmissionUpdateRequestDto();
        request.setIsTutorReviewRequested(true);

        when(userPracticeSubmissionRepository.findById("submission-1"))
                .thenReturn(Optional.of(submission));
        when(userPracticeSubmissionRepository.save(any(UserPracticeSubmission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserPracticeSubmissionResponseDto result =
                userPracticeSubmissionService.update("submission-1", request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("submission-1");
        assertThat(submission.isTutorReviewRequested()).isTrue();

        verify(userPracticeSubmissionRepository).findById("submission-1");
        verify(userPracticeSubmissionRepository).save(submission);
    }

    @Test
    void update_whenTutorReviewRequestedIsFalse_shouldUpdateFlagToFalseAndSave() {
        UserPracticeSubmission submission = fullSubmission("submission-1");
        submission.setTutorReviewRequested(true);

        UserPracticeSubmissionUpdateRequestDto request = new UserPracticeSubmissionUpdateRequestDto();
        request.setIsTutorReviewRequested(false);

        when(userPracticeSubmissionRepository.findById("submission-1"))
                .thenReturn(Optional.of(submission));
        when(userPracticeSubmissionRepository.save(any(UserPracticeSubmission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserPracticeSubmissionResponseDto result =
                userPracticeSubmissionService.update("submission-1", request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("submission-1");
        assertThat(submission.isTutorReviewRequested()).isFalse();

        verify(userPracticeSubmissionRepository).save(submission);
    }

    @Test
    void update_whenRequestContainsNullFields_shouldKeepExistingValuesAndStillSave() {
        UserPracticeSubmission submission = fullSubmission("submission-1");
        submission.setTutorReviewRequested(true);

        UserPracticeSubmissionUpdateRequestDto request = new UserPracticeSubmissionUpdateRequestDto();

        when(userPracticeSubmissionRepository.findById("submission-1"))
                .thenReturn(Optional.of(submission));
        when(userPracticeSubmissionRepository.save(any(UserPracticeSubmission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserPracticeSubmissionResponseDto result =
                userPracticeSubmissionService.update("submission-1", request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("submission-1");
        assertThat(submission.isTutorReviewRequested()).isTrue();

        verify(userPracticeSubmissionRepository).findById("submission-1");
        verify(userPracticeSubmissionRepository).save(submission);
    }

    @Test
    void update_whenSubmissionDoesNotExist_shouldThrowRuntimeExceptionAndSkipSave() {
        UserPracticeSubmissionUpdateRequestDto request = new UserPracticeSubmissionUpdateRequestDto();
        request.setIsTutorReviewRequested(true);

        when(userPracticeSubmissionRepository.findById("missing-submission"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userPracticeSubmissionService.update("missing-submission", request)
        );

        assertThat(exception.getMessage())
                .isEqualTo("User practice submission not found with id: missing-submission");

        verify(userPracticeSubmissionRepository).findById("missing-submission");
        verify(userPracticeSubmissionRepository, never()).save(any(UserPracticeSubmission.class));
    }

    @Test
    void syncTutorStatusFromTutorSubmissions_whenCompletedTutorExists_shouldSetCompletedAndSkipInReviewCheck() {
        UserPracticeSubmission submission = fullSubmission("submission-1");

        when(userPracticeSubmissionRepository.findByIdForUpdate("submission-1"))
                .thenReturn(Optional.of(submission));
        when(tutorUserPracticeSubmissionRepository.existsByUserPracticeSubmission_IdAndTutorStatus(
                "submission-1",
                TutorStatus.COMPLETED
        )).thenReturn(true);

        userPracticeSubmissionService.syncTutorStatusFromTutorSubmissions("submission-1");

        assertThat(submission.getTutorStatus()).isEqualTo(TutorStatus.COMPLETED);

        verify(userPracticeSubmissionRepository).findByIdForUpdate("submission-1");
        verify(tutorUserPracticeSubmissionRepository).existsByUserPracticeSubmission_IdAndTutorStatus(
                "submission-1",
                TutorStatus.COMPLETED
        );
        verify(tutorUserPracticeSubmissionRepository, never()).existsByUserPracticeSubmission_IdAndTutorStatus(
                "submission-1",
                TutorStatus.IN_REVIEW
        );
        verify(userPracticeSubmissionRepository).save(submission);
    }

    @Test
    void syncTutorStatusFromTutorSubmissions_whenNoCompletedTutorButInReviewTutorExists_shouldSetInReview() {
        UserPracticeSubmission submission = fullSubmission("submission-1");

        when(userPracticeSubmissionRepository.findByIdForUpdate("submission-1"))
                .thenReturn(Optional.of(submission));
        when(tutorUserPracticeSubmissionRepository.existsByUserPracticeSubmission_IdAndTutorStatus(
                "submission-1",
                TutorStatus.COMPLETED
        )).thenReturn(false);
        when(tutorUserPracticeSubmissionRepository.existsByUserPracticeSubmission_IdAndTutorStatus(
                "submission-1",
                TutorStatus.IN_REVIEW
        )).thenReturn(true);

        userPracticeSubmissionService.syncTutorStatusFromTutorSubmissions("submission-1");

        assertThat(submission.getTutorStatus()).isEqualTo(TutorStatus.IN_REVIEW);

        verify(tutorUserPracticeSubmissionRepository).existsByUserPracticeSubmission_IdAndTutorStatus(
                "submission-1",
                TutorStatus.COMPLETED
        );
        verify(tutorUserPracticeSubmissionRepository).existsByUserPracticeSubmission_IdAndTutorStatus(
                "submission-1",
                TutorStatus.IN_REVIEW
        );
        verify(userPracticeSubmissionRepository).save(submission);
    }

    @Test
    void syncTutorStatusFromTutorSubmissions_whenNoActiveTutorStatusExists_shouldSetPending() {
        UserPracticeSubmission submission = fullSubmission("submission-1");
        submission.setTutorStatus(TutorStatus.IN_REVIEW);

        when(userPracticeSubmissionRepository.findByIdForUpdate("submission-1"))
                .thenReturn(Optional.of(submission));
        when(tutorUserPracticeSubmissionRepository.existsByUserPracticeSubmission_IdAndTutorStatus(
                "submission-1",
                TutorStatus.COMPLETED
        )).thenReturn(false);
        when(tutorUserPracticeSubmissionRepository.existsByUserPracticeSubmission_IdAndTutorStatus(
                "submission-1",
                TutorStatus.IN_REVIEW
        )).thenReturn(false);

        userPracticeSubmissionService.syncTutorStatusFromTutorSubmissions("submission-1");

        assertThat(submission.getTutorStatus()).isEqualTo(TutorStatus.PENDING);

        verify(userPracticeSubmissionRepository).save(submission);
    }

    @Test
    void syncTutorStatusFromTutorSubmissions_whenSubmissionDoesNotExist_shouldThrowRuntimeExceptionAndSkipStatusLookup() {
        when(userPracticeSubmissionRepository.findByIdForUpdate("missing-submission"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userPracticeSubmissionService.syncTutorStatusFromTutorSubmissions("missing-submission")
        );

        assertThat(exception.getMessage())
                .isEqualTo("User practice submission not found with id: missing-submission");

        verify(userPracticeSubmissionRepository).findByIdForUpdate("missing-submission");
        verify(tutorUserPracticeSubmissionRepository, never()).existsByUserPracticeSubmission_IdAndTutorStatus(
                anyString(),
                any(TutorStatus.class)
        );
        verify(userPracticeSubmissionRepository, never()).save(any(UserPracticeSubmission.class));
    }

    @Test
    void delete_whenSubmissionExists_shouldDeleteSubmission() {
        UserPracticeSubmission submission = fullSubmission("submission-1");

        when(userPracticeSubmissionRepository.findById("submission-1"))
                .thenReturn(Optional.of(submission));

        userPracticeSubmissionService.delete("submission-1");

        verify(userPracticeSubmissionRepository).findById("submission-1");
        verify(userPracticeSubmissionRepository).delete(submission);
    }

    @Test
    void delete_whenSubmissionDoesNotExist_shouldThrowRuntimeExceptionAndSkipDelete() {
        when(userPracticeSubmissionRepository.findById("missing-submission"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userPracticeSubmissionService.delete("missing-submission")
        );

        assertThat(exception.getMessage())
                .isEqualTo("User practice submission not found with id: missing-submission");

        verify(userPracticeSubmissionRepository).findById("missing-submission");
        verify(userPracticeSubmissionRepository, never()).delete(any(UserPracticeSubmission.class));
    }

    private UserPracticeSubmissionCreateRequestDto createRequest() {
        UserPracticeSubmissionCreateRequestDto request = new UserPracticeSubmissionCreateRequestDto();

        request.setUserId("user-1");
        request.setPracticeContentId("content-1");
        request.setTimeSpentSeconds(120);
        request.setLearnerTestActivities(List.of(learnerActivity()));

        return request;
    }

    private LearnerTestActivityCreateRequestDto learnerActivity() {
        LearnerTestActivityCreateRequestDto activity = new LearnerTestActivityCreateRequestDto();

        activity.setActivityType(LearnerTestActivityType.TEST_START);
        activity.setQuestionNumber(1);
        activity.setValue("start");
        activity.setOffsetMs(0L);

        return activity;
    }

    private UserPracticeSubmission fullSubmission(String id) {
        UserPracticeSubmission submission = new UserPracticeSubmission();

        submission.setId(id);
        submission.setUserId("user-1");
        submission.setPracticeContentId("content-1");
        submission.setTimeSpentSeconds(120);
        submission.setSubmittedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        submission.setScore(8.0);
        submission.setCorrectAnswerPercentage(75.0);
        submission.setCorrectAnswerCount(15);
        submission.setWrongAnswerCount(4);
        submission.setSkipAnswerCount(1);
        submission.setTutorReviewRequested(true);
        submission.setTutorStatus(TutorStatus.IN_REVIEW);
        submission.setUser(fullUser());
        submission.setPracticeContent(fullPracticeContent());
        submission.setQuestionTypeAccuracies(List.of(questionTypeAccuracy()));

        return submission;
    }

    private User fullUser() {
        User user = new User();

        user.setUserId("user-1");
        user.setEmail("john@example.com");
        user.setFirstname("John");
        user.setLastname("Doe");

        return user;
    }

    private PracticeContent fullPracticeContent() {
        PracticeContent content = new PracticeContent();

        setIfPresent(content, "setId", "content-1");
        content.setSkill(PracticeContentSkill.LISTENING);
        content.setTask(PracticeTaskType.TASK_1);
        content.setTitle("Listening Practice");

        return content;
    }

    private SubmissionQuestionTypeAccuracy questionTypeAccuracy() {
        SubmissionQuestionTypeAccuracy accuracy = new SubmissionQuestionTypeAccuracy();

        accuracy.setQuestionType(PracticeQuestionType.MULTIPLE_CHOICE);
        accuracy.setCorrectAnswerPercentage(75.0);

        return accuracy;
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
