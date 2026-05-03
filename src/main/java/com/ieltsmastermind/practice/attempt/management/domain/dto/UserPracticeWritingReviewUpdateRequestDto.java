package com.ieltsmastermind.practice.attempt.management.domain.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPracticeWritingReviewUpdateRequestDto {

    @DecimalMin(value = "0.0", message = "Tutor task achievement band must be at least 0.0")
    @DecimalMax(value = "9.0", message = "Tutor task achievement band must be at most 9.0")
    private Double tutorTaskAchievementBand;

    @DecimalMin(value = "0.0", message = "Tutor task response band must be at least 0.0")
    @DecimalMax(value = "9.0", message = "Tutor task response band must be at most 9.0")
    private Double tutorTaskResponseBand;

    @DecimalMin(value = "0.0", message = "Tutor coherence and cohesion band must be at least 0.0")
    @DecimalMax(value = "9.0", message = "Tutor coherence and cohesion band must be at most 9.0")
    private Double tutorCoherenceAndCohesionBand;

    @DecimalMin(value = "0.0", message = "Tutor lexical resource band must be at least 0.0")
    @DecimalMax(value = "9.0", message = "Tutor lexical resource band must be at most 9.0")
    private Double tutorLexicalResourceBand;

    @DecimalMin(value = "0.0", message = "Tutor grammatical range and accuracy band must be at least 0.0")
    @DecimalMax(value = "9.0", message = "Tutor grammatical range and accuracy band must be at most 9.0")
    private Double tutorGrammaticalRangeAndAccuracyBand;
}
