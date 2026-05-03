package com.ieltsmastermind.practice.studyplan.management.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LearnerStudyPlanUserResponseDto {

    private String userId;
    private String email;
    private String firstname;
    private String lastname;
}
