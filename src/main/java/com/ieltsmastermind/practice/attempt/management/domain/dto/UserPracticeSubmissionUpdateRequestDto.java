package com.ieltsmastermind.practice.attempt.management.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserPracticeSubmissionUpdateRequestDto {

    private Boolean isTutorReviewRequested;
}
