package com.ieltsmastermind.practice.attempt.management.domain.dto;

import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmissionQuestionTypeAccuracyResponseDto {

    private PracticeQuestionType questionType;
    private Double correctAnswerPercentage;
}
