package com.ieltsmastermind.practice.studyplan.management.domain.dto;

import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LearnerStudyPlanResponseDto {

    private String id;
    private PracticeContentSkill skill;
    private Integer versionNumber;
    private LearnerStudyPlanStatus status;
    private Integer submissionCountUsed;
    private Integer submissionCountSinceCreation;
    private Boolean readyToFinalize;

    private LearnerStudyPlanUserResponseDto user;

    private List<LearnerStudyPlanTaskResponseDto> tasks;
    private List<LearnerStudyPlanStrengthBlockResponseDto> strengthBlocks;
    private List<LearnerStudyPlanWeaknessBlockResponseDto> weaknessBlocks;
}
