package com.ieltsmastermind.ai.feedback.business.interfaces;

import com.ieltsmastermind.ai.feedback.domain.dto.*;

public interface ListeningFeedbackService {

    void createListeningFeedback(String submissionId);
    ListeningFeedbackResponseDto getListeningFeedback(String submissionId);

}
