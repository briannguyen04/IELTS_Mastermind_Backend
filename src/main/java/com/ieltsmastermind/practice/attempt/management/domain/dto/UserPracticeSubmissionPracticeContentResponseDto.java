package com.ieltsmastermind.practice.attempt.management.domain.dto;

import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTaskType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPracticeSubmissionPracticeContentResponseDto {

    private String id;
    private PracticeContentSkill skill;
    private PracticeTaskType task;
    private String title;
}
