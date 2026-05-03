package com.ieltsmastermind.practice.attempt.management.domain.dto;

import com.ieltsmastermind.practice.attempt.management.domain.enums.Result;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserPracticeSubmissionAnswerResponseDto {

    private String id;
    private String submissionId;
    private Integer orderIndex;
    private List<String> answers;
    private Result result;
}
