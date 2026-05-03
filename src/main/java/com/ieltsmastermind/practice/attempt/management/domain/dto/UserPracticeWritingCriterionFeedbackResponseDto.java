package com.ieltsmastermind.practice.attempt.management.domain.dto;

import com.ieltsmastermind.practice.attempt.management.domain.enums.FeedbackAuthorType;
import com.ieltsmastermind.practice.attempt.management.domain.enums.WritingCriterionName;
import com.ieltsmastermind.practice.attempt.management.domain.enums.WritingFeedbackLabel;
import com.ieltsmastermind.practice.attempt.management.domain.enums.WritingFeedbackType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class UserPracticeWritingCriterionFeedbackResponseDto {

    private String id;
    private FeedbackAuthorType authorType;
    private WritingCriterionName criterionName;
    private WritingFeedbackType feedbackType;
    private WritingFeedbackLabel label;
    private String description;
    private String explanation;
    private List<String> evidenceSentences;
    private String recommendedActionDescription;
    private String recommendedActionExplanation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private UserPracticeWritingCriterionFeedbackUserResponseDto reviewedByUser;
}
