package com.ieltsmastermind.practice.attempt.management.business.interfaces;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeContentProgressResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeContentProgressUpsertRequestDto;

import java.util.List;

public interface UserPracticeContentProgressService {
    UserPracticeContentProgressResponseDto upsert(
            String userId,
            String practiceContentId,
            UserPracticeContentProgressUpsertRequestDto request
    );
    List<UserPracticeContentProgressResponseDto> getAllByUserId(String userId, IncludeSpec includes);
    UserPracticeContentProgressResponseDto incrementAttemptCount(String userId, String practiceContentId);
}
