package com.ieltsmastermind.practice.attempt.management.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPracticeSubmissionSkillCountResponseDto {

    private long listeningCount;
    private long readingCount;
    private long writingCount;
    private long speakingCount;
}
