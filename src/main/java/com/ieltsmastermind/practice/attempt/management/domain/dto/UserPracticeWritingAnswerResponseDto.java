package com.ieltsmastermind.practice.attempt.management.domain.dto;

import com.ieltsmastermind.practice.attempt.management.domain.enums.Result;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserPracticeWritingAnswerResponseDto {

    private String id;
    private String submissionId;
    private Integer orderIndex;
    private String essayText;
    private Integer wordCount;
}
