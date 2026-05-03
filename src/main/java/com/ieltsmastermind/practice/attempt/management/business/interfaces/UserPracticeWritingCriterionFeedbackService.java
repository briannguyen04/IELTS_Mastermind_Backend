package com.ieltsmastermind.practice.attempt.management.business.interfaces;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeWritingCriterionFeedbackCreateRequestDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeWritingCriterionFeedbackResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeWritingCriterionFeedbackUpdateRequestDto;

import java.util.List;

public interface UserPracticeWritingCriterionFeedbackService {

    UserPracticeWritingCriterionFeedbackResponseDto create(
            UserPracticeWritingCriterionFeedbackCreateRequestDto request
    );
    List<UserPracticeWritingCriterionFeedbackResponseDto> getAllBySubmissionId(
            String submissionId,
            IncludeSpec includes
    );
    UserPracticeWritingCriterionFeedbackResponseDto update(
            String id,
            UserPracticeWritingCriterionFeedbackUpdateRequestDto request
    );
    void delete(String id);
}
