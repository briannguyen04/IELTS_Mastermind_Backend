package com.ieltsmastermind.practice.attempt.management.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPracticeContentProgressResponseDto {

    private String id;
    private String userId;
    private String practiceContentId;
    private Boolean isBookmarked;
    private Integer attemptCount;
}
