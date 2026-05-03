package com.ieltsmastermind.practice.attempt.management.domain.dto;

import com.ieltsmastermind.practice.attempt.management.domain.enums.WritingFeedbackLabel;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserPracticeWritingCriterionFeedbackUpdateRequestDto {

    private WritingFeedbackLabel label;
    private String description;
    private String explanation;
    private List<String> evidenceSentences;
    private String recommendedActionDescription;
    private String recommendedActionExplanation;
}
