package com.ieltsmastermind.practice.attempt.management.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class UserPracticeSubmissionCreateRequestDto {

    private String userId;
    private String practiceContentId;
    private Integer timeSpentSeconds;
    private List<LearnerTestActivityCreateRequestDto> learnerTestActivities;
}
