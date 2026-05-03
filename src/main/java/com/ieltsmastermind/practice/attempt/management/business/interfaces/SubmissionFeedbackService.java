package com.ieltsmastermind.practice.attempt.management.business.interfaces;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.attempt.management.domain.dto.SubmissionFeedbackCreateRequestDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.SubmissionFeedbackResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.SubmissionFeedbackUpdateRequestDto;

import java.util.List;

public interface  SubmissionFeedbackService {

    SubmissionFeedbackResponseDto create(SubmissionFeedbackCreateRequestDto request);
    List<SubmissionFeedbackResponseDto> getAllBySubmissionId(String submissionId, IncludeSpec includes);
    SubmissionFeedbackResponseDto update(String id, SubmissionFeedbackUpdateRequestDto request);
    void delete(String id);
}
