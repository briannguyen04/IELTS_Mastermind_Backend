package com.ieltsmastermind.practice.attempt.management.business.interfaces;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeSubmissionCreateRequestDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeSubmissionResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeSubmissionUpdateRequestDto;

import java.util.List;

public interface UserPracticeSubmissionService {

    UserPracticeSubmissionResponseDto create(UserPracticeSubmissionCreateRequestDto request);
    List<UserPracticeSubmissionResponseDto> getAllByUserId(String userId, IncludeSpec includes);
    UserPracticeSubmissionResponseDto getById(String id, IncludeSpec includes);
    List<UserPracticeSubmissionResponseDto> getAll(IncludeSpec includes);
    List<UserPracticeSubmissionResponseDto> getAllTutorReviewRequested(IncludeSpec includes);
    List<UserPracticeSubmissionResponseDto> getAllByUserIdAndDays(String userId, int days, IncludeSpec includes);
    UserPracticeSubmissionResponseDto update(String id, UserPracticeSubmissionUpdateRequestDto request);
    void syncTutorStatusFromTutorSubmissions(String userPracticeSubmissionId);
    void delete(String id);
}
