package com.ieltsmastermind.practice.attempt.management.business.interfaces;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeWritingAnswerCreateRequestDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeWritingAnswerResponseDto;

import java.util.List;

public interface UserPracticeWritingAnswerService {

    UserPracticeWritingAnswerResponseDto create(
            UserPracticeWritingAnswerCreateRequestDto request
    );
    List<UserPracticeWritingAnswerResponseDto> getAllBySubmissionId(
            String submissionId,
            IncludeSpec includes
    );
    UserPracticeWritingAnswerResponseDto getById(String id, IncludeSpec includes);
}
