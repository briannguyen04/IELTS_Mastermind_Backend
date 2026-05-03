package com.ieltsmastermind.practice.studyplan.management.domain.dto;

import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LearnerStudyPlanCreateRequestDto {

    private String learnerId;
    private PracticeContentSkill skill;
}
