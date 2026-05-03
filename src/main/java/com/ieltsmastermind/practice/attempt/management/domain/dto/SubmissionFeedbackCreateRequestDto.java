package com.ieltsmastermind.practice.attempt.management.domain.dto;

import com.ieltsmastermind.practice.attempt.management.domain.enums.FeedbackAuthorType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmissionFeedbackCreateRequestDto {

    private String submissionId;
    private String authorId;
    private FeedbackAuthorType authorType;
    private String feedbackContent;
}
