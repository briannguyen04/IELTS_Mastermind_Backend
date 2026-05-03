package com.ieltsmastermind.practice.attempt.management.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPracticeWritingReviewResponseDto {

    private String id;
    private Double overallTutorBand;
    private Double tutorTaskResponseBand;
    private Double tutorTaskAchievementBand;
    private Double tutorCoherenceAndCohesionBand;
    private Double tutorLexicalResourceBand;
    private Double tutorGrammaticalRangeAndAccuracyBand;

    private UserPracticeWritingReviewUserResponseDto reviewedByUser;
}
