package com.ieltsmastermind.ai.feedback.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QuestionDto {
    private Integer questionNumber;
    private String result;
    private List<String> correctAnswer;
    private EvidenceDto evidence;
}