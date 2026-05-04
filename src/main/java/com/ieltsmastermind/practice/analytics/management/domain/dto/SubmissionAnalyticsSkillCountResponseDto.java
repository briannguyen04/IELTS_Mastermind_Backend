package com.ieltsmastermind.practice.analytics.management.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmissionAnalyticsSkillCountResponseDto {

    private Long listeningCount;
    private Long readingCount;
    private Long writingCount;
    private Long speakingCount;
}
