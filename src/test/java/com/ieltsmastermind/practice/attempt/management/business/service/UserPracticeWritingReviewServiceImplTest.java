package com.ieltsmastermind.practice.attempt.management.business.service;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeWritingReviewResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeWritingReviewUpdateRequestDto;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeWritingAnswer;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeWritingReview;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeWritingAnswerRepository;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeWritingReviewRepository;
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
class UserPracticeWritingReviewServiceImplTest {

    @Mock
    private UserPracticeWritingReviewRepository userPracticeWritingReviewRepository;

    @Mock
    private UserPracticeWritingAnswerRepository userPracticeWritingAnswerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IncludeSpec includes;

    @InjectMocks
    private UserPracticeWritingReviewServiceImpl writingReviewService;

    @Test
    void getAllByWritingAnswerId_whenReviewsExistAndFieldsAreIncluded_shouldReturnDtosWithIncludedFields() {
        UserPracticeWritingReview review = fullReview("review-1");

        when(userPracticeWritingReviewRepository.findAllByWritingAnswer_IdOrderByCreatedAtAsc("writing-answer-1"))
                .thenReturn(List.of(review));
        mockIncludes(
                "overalltutorband",
                "tutortaskresponseband",
                "tutortaskachievementband",
                "tutorcoherenceandcohesionband",
                "tutorlexicalresourceband",
                "tutorgrammaticalrangeandaccuracyband",
                "reviewedbyuser.userid",
                "reviewedbyuser.email",
                "reviewedbyuser.firstname",
                "reviewedbyuser.lastname"
        );

        List<UserPracticeWritingReviewResponseDto> result =
                writingReviewService.getAllByWritingAnswerId("writing-answer-1", includes);

        assertThat(result).hasSize(1);

        UserPracticeWritingReviewResponseDto dto = result.get(0);

        assertThat(dto.getId()).isEqualTo("review-1");
        assertThat(dto.getOverallTutorBand()).isEqualTo(8.0);
        assertThat(dto.getTutorTaskResponseBand()).isEqualTo(8.0);
        assertThat(dto.getTutorTaskAchievementBand()).isEqualTo(7.5);
        assertThat(dto.getTutorCoherenceAndCohesionBand()).isEqualTo(8.0);
        assertThat(dto.getTutorLexicalResourceBand()).isEqualTo(7.0);
        assertThat(dto.getTutorGrammaticalRangeAndAccuracyBand()).isEqualTo(7.5);

        assertThat(dto.getReviewedByUser()).isNotNull();
        assertThat(dto.getReviewedByUser().getUserId()).isEqualTo("reviewer-1");
        assertThat(dto.getReviewedByUser().getEmail()).isEqualTo("reviewer@example.com");
        assertThat(dto.getReviewedByUser().getFirstname()).isEqualTo("Review");
        assertThat(dto.getReviewedByUser().getLastname()).isEqualTo("Tutor");

        verify(userPracticeWritingReviewRepository)
                .findAllByWritingAnswer_IdOrderByCreatedAtAsc("writing-answer-1");
    }

    @Test
    void getAllByWritingAnswerId_whenNoFieldsAreIncluded_shouldReturnOnlyIdAndEmptyReviewedByUserDto() {
        UserPracticeWritingReview review = fullReview("review-1");

        when(userPracticeWritingReviewRepository.findAllByWritingAnswer_IdOrderByCreatedAtAsc("writing-answer-1"))
                .thenReturn(List.of(review));
        mockIncludes();

        List<UserPracticeWritingReviewResponseDto> result =
                writingReviewService.getAllByWritingAnswerId("writing-answer-1", includes);

        assertThat(result).hasSize(1);

        UserPracticeWritingReviewResponseDto dto = result.get(0);

        assertThat(dto.getId()).isEqualTo("review-1");
        assertThat(dto.getOverallTutorBand()).isNull();
        assertThat(dto.getTutorTaskResponseBand()).isNull();
        assertThat(dto.getTutorTaskAchievementBand()).isNull();
        assertThat(dto.getTutorCoherenceAndCohesionBand()).isNull();
        assertThat(dto.getTutorLexicalResourceBand()).isNull();
        assertThat(dto.getTutorGrammaticalRangeAndAccuracyBand()).isNull();

        assertThat(dto.getReviewedByUser()).isNotNull();
        assertThat(dto.getReviewedByUser().getUserId()).isNull();
        assertThat(dto.getReviewedByUser().getEmail()).isNull();
        assertThat(dto.getReviewedByUser().getFirstname()).isNull();
        assertThat(dto.getReviewedByUser().getLastname()).isNull();

        verify(userPracticeWritingReviewRepository)
                .findAllByWritingAnswer_IdOrderByCreatedAtAsc("writing-answer-1");
    }

    @Test
    void getAllByWritingAnswerId_whenRepositoryReturnsEmptyList_shouldReturnEmptyList() {
        when(userPracticeWritingReviewRepository.findAllByWritingAnswer_IdOrderByCreatedAtAsc("writing-answer-1"))
                .thenReturn(List.of());

        List<UserPracticeWritingReviewResponseDto> result =
                writingReviewService.getAllByWritingAnswerId("writing-answer-1", includes);

        assertThat(result).isEmpty();

        verify(userPracticeWritingReviewRepository)
                .findAllByWritingAnswer_IdOrderByCreatedAtAsc("writing-answer-1");
    }

    @Test
    void upsert_whenExistingReviewExistsAndAllBandsProvided_shouldUpdateBandsCalculateOverallSaveAndReturnReviewId() {
        String reviewedByUserId = "reviewer-1";
        String writingAnswerId = "writing-answer-1";
        UserPracticeWritingReview existingReview = fullReview("review-1");
        UserPracticeWritingReviewUpdateRequestDto request = fullUpdateRequest();

        when(userPracticeWritingReviewRepository.findByReviewedByUser_UserIdAndWritingAnswer_Id(
                reviewedByUserId,
                writingAnswerId
        )).thenReturn(Optional.of(existingReview));
        when(userPracticeWritingReviewRepository.save(any(UserPracticeWritingReview.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserPracticeWritingReviewResponseDto result =
                writingReviewService.upsert(reviewedByUserId, writingAnswerId, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("review-1");

        ArgumentCaptor<UserPracticeWritingReview> reviewCaptor =
                ArgumentCaptor.forClass(UserPracticeWritingReview.class);
        verify(userPracticeWritingReviewRepository).save(reviewCaptor.capture());

        UserPracticeWritingReview savedReview = reviewCaptor.getValue();

        assertThat(savedReview).isSameAs(existingReview);
        assertThat(savedReview.getTutorTaskAchievementBand()).isEqualTo(8.0);
        assertThat(savedReview.getTutorTaskResponseBand()).isEqualTo(7.5);
        assertThat(savedReview.getTutorCoherenceAndCohesionBand()).isEqualTo(7.0);
        assertThat(savedReview.getTutorLexicalResourceBand()).isEqualTo(8.5);
        assertThat(savedReview.getTutorGrammaticalRangeAndAccuracyBand()).isEqualTo(7.5);
        assertThat(savedReview.getOverallTutorBand()).isEqualTo(9.625);

        verify(userPracticeWritingReviewRepository).findByReviewedByUser_UserIdAndWritingAnswer_Id(
                reviewedByUserId,
                writingAnswerId
        );
        verify(userPracticeWritingAnswerRepository, never()).findById(anyString());
        verify(userRepository, never()).findById(anyString());
    }

    @Test
    void upsert_whenExistingReviewExistsAndRequestContainsOnlyNullFields_shouldKeepBandsCalculateOverallSaveAndReturnReviewId() {
        String reviewedByUserId = "reviewer-1";
        String writingAnswerId = "writing-answer-1";
        UserPracticeWritingReview existingReview = fullReview("review-1");
        UserPracticeWritingReviewUpdateRequestDto request = new UserPracticeWritingReviewUpdateRequestDto();

        when(userPracticeWritingReviewRepository.findByReviewedByUser_UserIdAndWritingAnswer_Id(
                reviewedByUserId,
                writingAnswerId
        )).thenReturn(Optional.of(existingReview));
        when(userPracticeWritingReviewRepository.save(any(UserPracticeWritingReview.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserPracticeWritingReviewResponseDto result =
                writingReviewService.upsert(reviewedByUserId, writingAnswerId, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("review-1");

        assertThat(existingReview.getTutorTaskAchievementBand()).isEqualTo(7.5);
        assertThat(existingReview.getTutorTaskResponseBand()).isEqualTo(8.0);
        assertThat(existingReview.getTutorCoherenceAndCohesionBand()).isEqualTo(8.0);
        assertThat(existingReview.getTutorLexicalResourceBand()).isEqualTo(7.0);
        assertThat(existingReview.getTutorGrammaticalRangeAndAccuracyBand()).isEqualTo(7.5);
        assertThat(existingReview.getOverallTutorBand()).isEqualTo(9.5);

        verify(userPracticeWritingReviewRepository).save(existingReview);
        verify(userPracticeWritingAnswerRepository, never()).findById(anyString());
        verify(userRepository, never()).findById(anyString());
    }

    @Test
    void upsert_whenExistingReviewExistsAndOnlyOneBandProvided_shouldUpdateOnlyProvidedBandAndRecalculateOverall() {
        String reviewedByUserId = "reviewer-1";
        String writingAnswerId = "writing-answer-1";
        UserPracticeWritingReview existingReview = fullReview("review-1");
        UserPracticeWritingReviewUpdateRequestDto request = new UserPracticeWritingReviewUpdateRequestDto();
        request.setTutorTaskResponseBand(9.0);

        when(userPracticeWritingReviewRepository.findByReviewedByUser_UserIdAndWritingAnswer_Id(
                reviewedByUserId,
                writingAnswerId
        )).thenReturn(Optional.of(existingReview));
        when(userPracticeWritingReviewRepository.save(any(UserPracticeWritingReview.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserPracticeWritingReviewResponseDto result =
                writingReviewService.upsert(reviewedByUserId, writingAnswerId, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("review-1");

        assertThat(existingReview.getTutorTaskAchievementBand()).isEqualTo(7.5);
        assertThat(existingReview.getTutorTaskResponseBand()).isEqualTo(9.0);
        assertThat(existingReview.getTutorCoherenceAndCohesionBand()).isEqualTo(8.0);
        assertThat(existingReview.getTutorLexicalResourceBand()).isEqualTo(7.0);
        assertThat(existingReview.getTutorGrammaticalRangeAndAccuracyBand()).isEqualTo(7.5);
        assertThat(existingReview.getOverallTutorBand()).isEqualTo(9.75);

        verify(userPracticeWritingReviewRepository).save(existingReview);
    }

    @Test
    void upsert_whenReviewDoesNotExistAndWritingAnswerAndUserExist_shouldCreateReviewSetRelationshipsBandsSaveAndReturnId() {
        String reviewedByUserId = "reviewer-1";
        String writingAnswerId = "writing-answer-1";
        UserPracticeWritingAnswer writingAnswer = writingAnswer(writingAnswerId);
        User reviewedByUser = reviewedByUser(reviewedByUserId);
        UserPracticeWritingReviewUpdateRequestDto request = fullUpdateRequest();

        when(userPracticeWritingReviewRepository.findByReviewedByUser_UserIdAndWritingAnswer_Id(
                reviewedByUserId,
                writingAnswerId
        )).thenReturn(Optional.empty());
        when(userPracticeWritingAnswerRepository.findById(writingAnswerId))
                .thenReturn(Optional.of(writingAnswer));
        when(userRepository.findById(reviewedByUserId)).thenReturn(Optional.of(reviewedByUser));
        when(userPracticeWritingReviewRepository.save(any(UserPracticeWritingReview.class)))
                .thenAnswer(invocation -> {
                    UserPracticeWritingReview review = invocation.getArgument(0);
                    review.setId("review-1");
                    return review;
                });

        UserPracticeWritingReviewResponseDto result =
                writingReviewService.upsert(reviewedByUserId, writingAnswerId, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("review-1");

        ArgumentCaptor<UserPracticeWritingReview> reviewCaptor =
                ArgumentCaptor.forClass(UserPracticeWritingReview.class);
        verify(userPracticeWritingReviewRepository).save(reviewCaptor.capture());

        UserPracticeWritingReview savedReview = reviewCaptor.getValue();

        assertThat(savedReview.getWritingAnswer()).isSameAs(writingAnswer);
        assertThat(savedReview.getReviewedByUser()).isSameAs(reviewedByUser);
        assertThat(savedReview.getTutorTaskAchievementBand()).isEqualTo(8.0);
        assertThat(savedReview.getTutorTaskResponseBand()).isEqualTo(7.5);
        assertThat(savedReview.getTutorCoherenceAndCohesionBand()).isEqualTo(7.0);
        assertThat(savedReview.getTutorLexicalResourceBand()).isEqualTo(8.5);
        assertThat(savedReview.getTutorGrammaticalRangeAndAccuracyBand()).isEqualTo(7.5);
        assertThat(savedReview.getOverallTutorBand()).isEqualTo(9.625);

        verify(userPracticeWritingReviewRepository).findByReviewedByUser_UserIdAndWritingAnswer_Id(
                reviewedByUserId,
                writingAnswerId
        );
        verify(userPracticeWritingAnswerRepository).findById(writingAnswerId);
        verify(userRepository).findById(reviewedByUserId);
    }

    @Test
    void upsert_whenReviewDoesNotExistAndRequestContainsOnlyNullFields_shouldCreateReviewWithDefaultBandsAndOverallZero() {
        String reviewedByUserId = "reviewer-1";
        String writingAnswerId = "writing-answer-1";
        UserPracticeWritingAnswer writingAnswer = writingAnswer(writingAnswerId);
        User reviewedByUser = reviewedByUser(reviewedByUserId);
        UserPracticeWritingReviewUpdateRequestDto request = new UserPracticeWritingReviewUpdateRequestDto();

        when(userPracticeWritingReviewRepository.findByReviewedByUser_UserIdAndWritingAnswer_Id(
                reviewedByUserId,
                writingAnswerId
        )).thenReturn(Optional.empty());
        when(userPracticeWritingAnswerRepository.findById(writingAnswerId))
                .thenReturn(Optional.of(writingAnswer));
        when(userRepository.findById(reviewedByUserId)).thenReturn(Optional.of(reviewedByUser));
        when(userPracticeWritingReviewRepository.save(any(UserPracticeWritingReview.class)))
                .thenAnswer(invocation -> {
                    UserPracticeWritingReview review = invocation.getArgument(0);
                    review.setId("review-1");
                    return review;
                });

        UserPracticeWritingReviewResponseDto result =
                writingReviewService.upsert(reviewedByUserId, writingAnswerId, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("review-1");

        ArgumentCaptor<UserPracticeWritingReview> reviewCaptor =
                ArgumentCaptor.forClass(UserPracticeWritingReview.class);
        verify(userPracticeWritingReviewRepository).save(reviewCaptor.capture());

        UserPracticeWritingReview savedReview = reviewCaptor.getValue();

        assertThat(savedReview.getWritingAnswer()).isSameAs(writingAnswer);
        assertThat(savedReview.getReviewedByUser()).isSameAs(reviewedByUser);
        assertThat(savedReview.getTutorTaskAchievementBand()).isEqualTo(0.0);
        assertThat(savedReview.getTutorTaskResponseBand()).isEqualTo(0.0);
        assertThat(savedReview.getTutorCoherenceAndCohesionBand()).isEqualTo(0.0);
        assertThat(savedReview.getTutorLexicalResourceBand()).isEqualTo(0.0);
        assertThat(savedReview.getTutorGrammaticalRangeAndAccuracyBand()).isEqualTo(0.0);
        assertThat(savedReview.getOverallTutorBand()).isEqualTo(0.0);
    }

    @Test
    void upsert_whenReviewDoesNotExistAndWritingAnswerDoesNotExist_shouldThrowRuntimeExceptionAndSkipUserLookupAndSave() {
        String reviewedByUserId = "reviewer-1";
        String writingAnswerId = "missing-writing-answer";
        UserPracticeWritingReviewUpdateRequestDto request = fullUpdateRequest();

        when(userPracticeWritingReviewRepository.findByReviewedByUser_UserIdAndWritingAnswer_Id(
                reviewedByUserId,
                writingAnswerId
        )).thenReturn(Optional.empty());
        when(userPracticeWritingAnswerRepository.findById(writingAnswerId))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> writingReviewService.upsert(reviewedByUserId, writingAnswerId, request)
        );

        assertThat(exception.getMessage())
                .isEqualTo("User practice writing answer not found with id: missing-writing-answer");

        verify(userPracticeWritingReviewRepository).findByReviewedByUser_UserIdAndWritingAnswer_Id(
                reviewedByUserId,
                writingAnswerId
        );
        verify(userPracticeWritingAnswerRepository).findById(writingAnswerId);
        verify(userRepository, never()).findById(anyString());
        verify(userPracticeWritingReviewRepository, never()).save(any(UserPracticeWritingReview.class));
    }

    @Test
    void upsert_whenReviewDoesNotExistAndUserDoesNotExist_shouldThrowRuntimeExceptionAndSkipSave() {
        String reviewedByUserId = "missing-user";
        String writingAnswerId = "writing-answer-1";
        UserPracticeWritingAnswer writingAnswer = writingAnswer(writingAnswerId);
        UserPracticeWritingReviewUpdateRequestDto request = fullUpdateRequest();

        when(userPracticeWritingReviewRepository.findByReviewedByUser_UserIdAndWritingAnswer_Id(
                reviewedByUserId,
                writingAnswerId
        )).thenReturn(Optional.empty());
        when(userPracticeWritingAnswerRepository.findById(writingAnswerId))
                .thenReturn(Optional.of(writingAnswer));
        when(userRepository.findById(reviewedByUserId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> writingReviewService.upsert(reviewedByUserId, writingAnswerId, request)
        );

        assertThat(exception.getMessage())
                .isEqualTo("User not found with id: missing-user");

        verify(userPracticeWritingAnswerRepository).findById(writingAnswerId);
        verify(userRepository).findById(reviewedByUserId);
        verify(userPracticeWritingReviewRepository, never()).save(any(UserPracticeWritingReview.class));
    }

    private UserPracticeWritingReviewUpdateRequestDto fullUpdateRequest() {
        UserPracticeWritingReviewUpdateRequestDto request = new UserPracticeWritingReviewUpdateRequestDto();

        request.setTutorTaskAchievementBand(8.0);
        request.setTutorTaskResponseBand(7.5);
        request.setTutorCoherenceAndCohesionBand(7.0);
        request.setTutorLexicalResourceBand(8.5);
        request.setTutorGrammaticalRangeAndAccuracyBand(7.5);

        return request;
    }

    private UserPracticeWritingReview fullReview(String id) {
        UserPracticeWritingReview review = new UserPracticeWritingReview();

        review.setId(id);
        review.setOverallTutorBand(8.0);
        review.setTutorTaskAchievementBand(7.5);
        review.setTutorTaskResponseBand(8.0);
        review.setTutorCoherenceAndCohesionBand(8.0);
        review.setTutorLexicalResourceBand(7.0);
        review.setTutorGrammaticalRangeAndAccuracyBand(7.5);
        review.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        review.setUpdatedAt(LocalDateTime.of(2026, 1, 2, 10, 0));
        review.setWritingAnswer(writingAnswer("writing-answer-1"));
        review.setReviewedByUser(reviewedByUser("reviewer-1"));

        return review;
    }

    private UserPracticeWritingAnswer writingAnswer(String id) {
        UserPracticeWritingAnswer writingAnswer = new UserPracticeWritingAnswer();

        writingAnswer.setId(id);
        writingAnswer.setOrderIndex(1);
        writingAnswer.setEssayText("This is a sample essay.");
        writingAnswer.setWordCount(5);

        return writingAnswer;
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
