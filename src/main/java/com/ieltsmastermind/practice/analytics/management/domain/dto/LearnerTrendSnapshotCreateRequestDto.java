package com.ieltsmastermind.practice.analytics.management.domain.dto;

import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LearnerTrendSnapshotCreateRequestDto {

    private String learnerId;
    private PracticeContentSkill skill;
}
