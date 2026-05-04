package com.ieltsmastermind.practice.attempt.management.business.service;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeContentProgressResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeContentProgressUpsertRequestDto;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeContentProgress;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeContentProgressRepository;
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
class UserPracticeContentProgressServiceImplTest {

    @Mock
    private UserPracticeContentProgressRepository userPracticeContentProgressRepository;

    @Mock
    private IncludeSpec includes;

    @InjectMocks
    private UserPracticeContentProgressServiceImpl userPracticeContentProgressService;

    @Test
    void upsert_whenProgressExistsAndIsBookmarkedProvided_shouldUpdateBookmarkSaveAndReturnProgressId() {
        String userId = "user-1";
        String practiceContentId = "content-1";
        UserPracticeContentProgress existingProgress = progress("progress-1", userId, practiceContentId);
        existingProgress.setIsBookmarked(false);

        UserPracticeContentProgressUpsertRequestDto request = upsertRequest(true);

        when(userPracticeContentProgressRepository.findByUserIdAndPracticeContentId(userId, practiceContentId))
                .thenReturn(Optional.of(existingProgress));
        when(userPracticeContentProgressRepository.save(any(UserPracticeContentProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserPracticeContentProgressResponseDto result =
                userPracticeContentProgressService.upsert(userId, practiceContentId, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("progress-1");
        assertThat(existingProgress.getIsBookmarked()).isTrue();

        verify(userPracticeContentProgressRepository).findByUserIdAndPracticeContentId(userId, practiceContentId);
        verify(userPracticeContentProgressRepository).save(existingProgress);
    }

    @Test
    void upsert_whenProgressExistsAndIsBookmarkedIsNull_shouldKeepExistingBookmarkSaveAndReturnProgressId() {
        String userId = "user-1";
        String practiceContentId = "content-1";
        UserPracticeContentProgress existingProgress = progress("progress-1", userId, practiceContentId);
        existingProgress.setIsBookmarked(true);

        UserPracticeContentProgressUpsertRequestDto request = new UserPracticeContentProgressUpsertRequestDto();

        when(userPracticeContentProgressRepository.findByUserIdAndPracticeContentId(userId, practiceContentId))
                .thenReturn(Optional.of(existingProgress));
        when(userPracticeContentProgressRepository.save(any(UserPracticeContentProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserPracticeContentProgressResponseDto result =
                userPracticeContentProgressService.upsert(userId, practiceContentId, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("progress-1");
        assertThat(existingProgress.getIsBookmarked()).isTrue();

        verify(userPracticeContentProgressRepository).findByUserIdAndPracticeContentId(userId, practiceContentId);
        verify(userPracticeContentProgressRepository).save(existingProgress);
    }

    @Test
    void upsert_whenProgressDoesNotExistAndIsBookmarkedProvided_shouldCreateProgressSaveAndReturnProgressId() {
        String userId = "user-1";
        String practiceContentId = "content-1";
        UserPracticeContentProgressUpsertRequestDto request = upsertRequest(true);

        when(userPracticeContentProgressRepository.findByUserIdAndPracticeContentId(userId, practiceContentId))
                .thenReturn(Optional.empty());
        when(userPracticeContentProgressRepository.save(any(UserPracticeContentProgress.class)))
                .thenAnswer(invocation -> {
                    UserPracticeContentProgress progress = invocation.getArgument(0);
                    progress.setId("progress-1");
                    return progress;
                });

        UserPracticeContentProgressResponseDto result =
                userPracticeContentProgressService.upsert(userId, practiceContentId, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("progress-1");

        ArgumentCaptor<UserPracticeContentProgress> progressCaptor =
                ArgumentCaptor.forClass(UserPracticeContentProgress.class);
        verify(userPracticeContentProgressRepository).save(progressCaptor.capture());

        UserPracticeContentProgress savedProgress = progressCaptor.getValue();

        assertThat(savedProgress.getUserId()).isEqualTo(userId);
        assertThat(savedProgress.getPracticeContentId()).isEqualTo(practiceContentId);
        assertThat(savedProgress.getIsBookmarked()).isTrue();
        assertThat(savedProgress.getAttemptCount()).isZero();

        verify(userPracticeContentProgressRepository).findByUserIdAndPracticeContentId(userId, practiceContentId);
    }

    @Test
    void upsert_whenProgressDoesNotExistAndIsBookmarkedIsNull_shouldCreateProgressWithDefaultValuesSaveAndReturnProgressId() {
        String userId = "user-1";
        String practiceContentId = "content-1";
        UserPracticeContentProgressUpsertRequestDto request = new UserPracticeContentProgressUpsertRequestDto();

        when(userPracticeContentProgressRepository.findByUserIdAndPracticeContentId(userId, practiceContentId))
                .thenReturn(Optional.empty());
        when(userPracticeContentProgressRepository.save(any(UserPracticeContentProgress.class)))
                .thenAnswer(invocation -> {
                    UserPracticeContentProgress progress = invocation.getArgument(0);
                    progress.setId("progress-1");
                    return progress;
                });

        UserPracticeContentProgressResponseDto result =
                userPracticeContentProgressService.upsert(userId, practiceContentId, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("progress-1");

        ArgumentCaptor<UserPracticeContentProgress> progressCaptor =
                ArgumentCaptor.forClass(UserPracticeContentProgress.class);
        verify(userPracticeContentProgressRepository).save(progressCaptor.capture());

        UserPracticeContentProgress savedProgress = progressCaptor.getValue();

        assertThat(savedProgress.getUserId()).isEqualTo(userId);
        assertThat(savedProgress.getPracticeContentId()).isEqualTo(practiceContentId);
        assertThat(savedProgress.getIsBookmarked()).isFalse();
        assertThat(savedProgress.getAttemptCount()).isZero();

        verify(userPracticeContentProgressRepository).findByUserIdAndPracticeContentId(userId, practiceContentId);
    }

    @Test
    void upsert_whenRequestIsNull_shouldThrowNullPointerExceptionAndSkipSave() {
        String userId = "user-1";
        String practiceContentId = "content-1";
        UserPracticeContentProgress existingProgress = progress("progress-1", userId, practiceContentId);

        when(userPracticeContentProgressRepository.findByUserIdAndPracticeContentId(userId, practiceContentId))
                .thenReturn(Optional.of(existingProgress));

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> userPracticeContentProgressService.upsert(userId, practiceContentId, null)
        );

        assertThat(exception).isNotNull();

        verify(userPracticeContentProgressRepository).findByUserIdAndPracticeContentId(userId, practiceContentId);
        verify(userPracticeContentProgressRepository, never()).save(any(UserPracticeContentProgress.class));
    }

    @Test
    void incrementAttemptCount_whenExistingProgressIsUpdated_shouldFindProgressAndReturnProgressId() {
        String userId = "user-1";
        String practiceContentId = "content-1";
        UserPracticeContentProgress existingProgress = progress("progress-1", userId, practiceContentId);
        existingProgress.setAttemptCount(3);

        when(userPracticeContentProgressRepository.incrementAttemptCount(userId, practiceContentId))
                .thenReturn(1);
        when(userPracticeContentProgressRepository.findByUserIdAndPracticeContentId(userId, practiceContentId))
                .thenReturn(Optional.of(existingProgress));

        UserPracticeContentProgressResponseDto result =
                userPracticeContentProgressService.incrementAttemptCount(userId, practiceContentId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("progress-1");

        verify(userPracticeContentProgressRepository).incrementAttemptCount(userId, practiceContentId);
        verify(userPracticeContentProgressRepository).findByUserIdAndPracticeContentId(userId, practiceContentId);
        verify(userPracticeContentProgressRepository, never()).save(any(UserPracticeContentProgress.class));
    }

    @Test
    void incrementAttemptCount_whenNoExistingProgressIsUpdated_shouldCreateProgressWithAttemptCountOneAndReturnProgressId() {
        String userId = "user-1";
        String practiceContentId = "content-1";

        when(userPracticeContentProgressRepository.incrementAttemptCount(userId, practiceContentId))
                .thenReturn(0);
        when(userPracticeContentProgressRepository.save(any(UserPracticeContentProgress.class)))
                .thenAnswer(invocation -> {
                    UserPracticeContentProgress progress = invocation.getArgument(0);
                    progress.setId("progress-1");
                    return progress;
                });

        UserPracticeContentProgressResponseDto result =
                userPracticeContentProgressService.incrementAttemptCount(userId, practiceContentId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("progress-1");

        ArgumentCaptor<UserPracticeContentProgress> progressCaptor =
                ArgumentCaptor.forClass(UserPracticeContentProgress.class);
        verify(userPracticeContentProgressRepository).save(progressCaptor.capture());

        UserPracticeContentProgress savedProgress = progressCaptor.getValue();

        assertThat(savedProgress.getUserId()).isEqualTo(userId);
        assertThat(savedProgress.getPracticeContentId()).isEqualTo(practiceContentId);
        assertThat(savedProgress.getAttemptCount()).isEqualTo(1);
        assertThat(savedProgress.getIsBookmarked()).isFalse();

        verify(userPracticeContentProgressRepository).incrementAttemptCount(userId, practiceContentId);
        verify(userPracticeContentProgressRepository, never())
                .findByUserIdAndPracticeContentId(anyString(), anyString());
    }

    @Test
    void incrementAttemptCount_whenExistingProgressWasUpdatedButCannotBeFound_shouldThrowRuntimeExceptionAndSkipSave() {
        String userId = "user-1";
        String practiceContentId = "content-1";

        when(userPracticeContentProgressRepository.incrementAttemptCount(userId, practiceContentId))
                .thenReturn(1);
        when(userPracticeContentProgressRepository.findByUserIdAndPracticeContentId(userId, practiceContentId))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userPracticeContentProgressService.incrementAttemptCount(userId, practiceContentId)
        );

        assertThat(exception.getMessage()).isEqualTo("User practice content progress not found");

        verify(userPracticeContentProgressRepository).incrementAttemptCount(userId, practiceContentId);
        verify(userPracticeContentProgressRepository).findByUserIdAndPracticeContentId(userId, practiceContentId);
        verify(userPracticeContentProgressRepository, never()).save(any(UserPracticeContentProgress.class));
    }

    @Test
    void getAllByUserId_whenProgressRowsExistAndFieldsAreIncluded_shouldReturnDtosWithIncludedFields() {
        String userId = "user-1";
        UserPracticeContentProgress progress1 = progress("progress-1", userId, "content-1");
        progress1.setIsBookmarked(true);
        progress1.setAttemptCount(3);

        UserPracticeContentProgress progress2 = progress("progress-2", userId, "content-2");
        progress2.setIsBookmarked(false);
        progress2.setAttemptCount(1);

        when(userPracticeContentProgressRepository.findAllByUserId(userId))
                .thenReturn(List.of(progress1, progress2));
        mockIncludes("userid", "practicecontentid", "isbookmarked", "attemptcount");

        List<UserPracticeContentProgressResponseDto> result =
                userPracticeContentProgressService.getAllByUserId(userId, includes);

        assertThat(result).hasSize(2);

        assertThat(result.get(0).getId()).isEqualTo("progress-1");
        assertThat(result.get(0).getUserId()).isEqualTo(userId);
        assertThat(result.get(0).getPracticeContentId()).isEqualTo("content-1");
        assertThat(result.get(0).getIsBookmarked()).isTrue();
        assertThat(result.get(0).getAttemptCount()).isEqualTo(3);

        assertThat(result.get(1).getId()).isEqualTo("progress-2");
        assertThat(result.get(1).getUserId()).isEqualTo(userId);
        assertThat(result.get(1).getPracticeContentId()).isEqualTo("content-2");
        assertThat(result.get(1).getIsBookmarked()).isFalse();
        assertThat(result.get(1).getAttemptCount()).isEqualTo(1);

        verify(userPracticeContentProgressRepository).findAllByUserId(userId);
    }

    @Test
    void getAllByUserId_whenNoFieldsAreIncluded_shouldReturnOnlyIds() {
        String userId = "user-1";
        UserPracticeContentProgress progress = progress("progress-1", userId, "content-1");
        progress.setIsBookmarked(true);
        progress.setAttemptCount(3);

        when(userPracticeContentProgressRepository.findAllByUserId(userId))
                .thenReturn(List.of(progress));
        mockIncludes();

        List<UserPracticeContentProgressResponseDto> result =
                userPracticeContentProgressService.getAllByUserId(userId, includes);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("progress-1");
        assertThat(result.get(0).getUserId()).isNull();
        assertThat(result.get(0).getPracticeContentId()).isNull();
        assertThat(result.get(0).getIsBookmarked()).isNull();
        assertThat(result.get(0).getAttemptCount()).isNull();

        verify(userPracticeContentProgressRepository).findAllByUserId(userId);
    }

    @Test
    void getAllByUserId_whenRepositoryReturnsEmptyList_shouldReturnEmptyList() {
        String userId = "user-1";

        when(userPracticeContentProgressRepository.findAllByUserId(userId))
                .thenReturn(List.of());

        List<UserPracticeContentProgressResponseDto> result =
                userPracticeContentProgressService.getAllByUserId(userId, includes);

        assertThat(result).isEmpty();

        verify(userPracticeContentProgressRepository).findAllByUserId(userId);
    }

    private UserPracticeContentProgressUpsertRequestDto upsertRequest(Boolean isBookmarked) {
        UserPracticeContentProgressUpsertRequestDto request =
                new UserPracticeContentProgressUpsertRequestDto();

        request.setIsBookmarked(isBookmarked);

        return request;
    }

    private UserPracticeContentProgress progress(String id, String userId, String practiceContentId) {
        UserPracticeContentProgress progress = new UserPracticeContentProgress();

        progress.setId(id);
        progress.setUserId(userId);
        progress.setPracticeContentId(practiceContentId);
        progress.setIsBookmarked(false);
        progress.setAttemptCount(0);

        return progress;
    }

    private void mockIncludes(String... fieldsToInclude) {
        Set<String> includedFields = Set.copyOf(Arrays.asList(fieldsToInclude));

        when(includes.has(anyString())).thenAnswer(invocation -> {
            String fieldName = invocation.getArgument(0);
            return includedFields.contains(fieldName);
        });
    }
}
