package com.ieltsmastermind.ai.feedback.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
public class WritingFeedbackItemDto {
    private String criterionName;
    private String feedbackType;
    private String label;
    private String description;
    private String explanation;
    private List<String> evidenceSentences;
    private String recommendedActionDescription;
    private String recommendedActionExplanation;
}
