package com.ieltsmastermind.practice.attempt.management.domain.dto;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class UserPracticeSubmissionAnswerBulkCreateRequestDto {

    private String userPracticeSubmissionId;
    private List<UserPracticeSubmissionAnswerCreateRequestDto> answers = new ArrayList<>();
}