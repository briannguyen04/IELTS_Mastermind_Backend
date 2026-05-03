package com.ieltsmastermind.ai.feedback.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class WritingAnswerFeedbackDto {
    private Integer orderIndex;
    private List<WritingFeedbackItemDto> feedbacks;
}