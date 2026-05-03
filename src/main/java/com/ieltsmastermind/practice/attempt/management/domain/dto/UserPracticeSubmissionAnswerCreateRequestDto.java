package com.ieltsmastermind.practice.attempt.management.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class UserPracticeSubmissionAnswerCreateRequestDto {

    private String userPracticeSubmissionId;
    private Integer orderIndex;
    private List<String> answers = new ArrayList<>();
}
