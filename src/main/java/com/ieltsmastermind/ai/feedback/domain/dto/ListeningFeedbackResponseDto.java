package com.ieltsmastermind.ai.feedback.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ListeningFeedbackResponseDto {
    private String summary;
    private List<QuestionDto> questions;
}
