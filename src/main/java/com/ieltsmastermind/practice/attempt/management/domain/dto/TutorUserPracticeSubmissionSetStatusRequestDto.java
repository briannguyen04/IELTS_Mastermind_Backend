package com.ieltsmastermind.practice.attempt.management.domain.dto;

import com.ieltsmastermind.practice.attempt.management.domain.enums.TutorStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TutorUserPracticeSubmissionSetStatusRequestDto {

    private TutorStatus tutorStatus;
}
