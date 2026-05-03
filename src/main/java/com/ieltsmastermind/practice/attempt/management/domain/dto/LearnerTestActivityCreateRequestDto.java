package com.ieltsmastermind.practice.attempt.management.domain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ieltsmastermind.practice.attempt.management.domain.enums.LearnerTestActivityType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LearnerTestActivityCreateRequestDto {

    @JsonProperty("at")
    @JsonAlias("activityType")
    private LearnerTestActivityType activityType;

    @JsonProperty("qn")
    @JsonAlias("questionNumber")
    private Integer questionNumber;

    @JsonProperty("v")
    @JsonAlias("value")
    private String value;

    @JsonProperty("om")
    @JsonAlias("offsetMs")
    private Long offsetMs;
}
