package com.ieltsmastermind.ai.feedback.business.interfaces;

import com.ieltsmastermind.ai.feedback.domain.dto.ReadingFeedbackResponseDto;
import com.ieltsmastermind.ai.feedback.domain.dto.WritingFeedbackResponseDto;

public interface WritingFeedbackService {
    void createWritingFeedback(String submissionId);
    WritingFeedbackResponseDto getWritingFeedback(String submissionId);
}
