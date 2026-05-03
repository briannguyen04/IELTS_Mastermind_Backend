package com.ieltsmastermind.ai.feedback.business.service.LearnerPracticeAIService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


@Service
public class WritingFeedbackAsyncService {

    private static final Logger log = LoggerFactory.getLogger(WritingFeedbackAsyncService.class);
    @Autowired
    private WritingFeedbackServicelmpl writingService;

    @Async("aiExecutor")
    public void generateFeedback(String submissionId) {
        try {
            writingService.createWritingFeedback(submissionId);
        } catch (Exception e) {
            log.error("Failed to generate feedback for submissionId={}", submissionId, e);
        }
    }
}