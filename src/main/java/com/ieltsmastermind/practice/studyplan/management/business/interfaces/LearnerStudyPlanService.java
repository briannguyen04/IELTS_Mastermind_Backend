package com.ieltsmastermind.practice.studyplan.management.business.interfaces;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;
import com.ieltsmastermind.practice.studyplan.management.domain.dto.LearnerStudyPlanActiveCheckResponseDto;
import com.ieltsmastermind.practice.studyplan.management.domain.dto.LearnerStudyPlanCreateRequestDto;
import com.ieltsmastermind.practice.studyplan.management.domain.dto.LearnerStudyPlanRefreshAfterSubmissionResponseDto;
import com.ieltsmastermind.practice.studyplan.management.domain.dto.LearnerStudyPlanResponseDto;

import java.util.List;

public interface LearnerStudyPlanService {
    LearnerStudyPlanResponseDto create(LearnerStudyPlanCreateRequestDto request);
    LearnerStudyPlanResponseDto getActiveStudyPlanByUserIdAndSkill(
            String userId,
            PracticeContentSkill skill,
            IncludeSpec includes
    );
    LearnerStudyPlanActiveCheckResponseDto getHasActiveStudyPlan(
            String userId,
            PracticeContentSkill skill
    );
    LearnerStudyPlanResponseDto refreshStudyPlan(
            String userId,
            PracticeContentSkill skill
    );
    LearnerStudyPlanResponseDto finalizeStudyPlanById(String id);
}
