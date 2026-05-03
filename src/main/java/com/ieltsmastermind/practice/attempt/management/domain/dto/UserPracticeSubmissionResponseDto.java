package com.ieltsmastermind.practice.attempt.management.domain.dto;

import com.ieltsmastermind.practice.attempt.management.domain.enums.TutorStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class UserPracticeSubmissionResponseDto {

    private String id;
    private String userId;
    private String practiceContentId;
    private Integer timeSpentSeconds;
    private LocalDateTime submittedAt;
    private Double score;
    private Double correctAnswerPercentage;
    private Integer correctAnswerCount;
    private Integer wrongAnswerCount;
    private Integer skipAnswerCount;
    private Boolean isTutorReviewRequested;
    private TutorStatus tutorStatus;

    private UserPracticeSubmissionUserResponseDto user;
    private UserPracticeSubmissionPracticeContentResponseDto practiceContent;
    private List<SubmissionQuestionTypeAccuracyResponseDto> questionTypeAccuracies;
}
