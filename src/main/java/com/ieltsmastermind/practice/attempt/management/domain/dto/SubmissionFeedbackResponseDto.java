package com.ieltsmastermind.practice.attempt.management.domain.dto;

import com.ieltsmastermind.practice.attempt.management.domain.enums.FeedbackAuthorType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SubmissionFeedbackResponseDto {

    private String id;
    private FeedbackAuthorType authorType;
    private String feedbackContent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private SubmissionFeedbackAuthorResponseDto author;
}
