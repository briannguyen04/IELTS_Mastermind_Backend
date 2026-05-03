package com.ieltsmastermind.practice.attempt.management.business.interfaces;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeWritingReviewResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeWritingReviewUpdateRequestDto;

import java.util.List;

public interface UserPracticeWritingReviewService {

    List<UserPracticeWritingReviewResponseDto> getAllByWritingAnswerId(
            String writingAnswerId,
            IncludeSpec includes
    );
    UserPracticeWritingReviewResponseDto upsert(
            String reviewedByUserId,
            String writingAnswerId,
            UserPracticeWritingReviewUpdateRequestDto request
    );
}
