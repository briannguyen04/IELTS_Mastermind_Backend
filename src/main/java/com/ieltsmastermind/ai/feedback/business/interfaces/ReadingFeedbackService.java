package com.ieltsmastermind.ai.feedback.business.interfaces;

import com.ieltsmastermind.ai.feedback.domain.dto.ListeningFeedbackResponseDto;
import com.ieltsmastermind.ai.feedback.domain.dto.ReadingFeedbackResponseDto;

public interface ReadingFeedbackService {
    void createReadingFeedback(String submissionId);
    ReadingFeedbackResponseDto getReadingFeedback(String submissionId);
}
