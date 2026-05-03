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
    LearnerStudyPlanResponseDto getActiveByUserIdAndSkill(
            String userId,
            PracticeContentSkill skill,
            IncludeSpec includes
    );
    LearnerStudyPlanActiveCheckResponseDto checkHasActiveStudyPlan(
            String userId,
            PracticeContentSkill skill
    );
    void refreshStudyPlanIfStudyPlanExists(
            String userId,
            PracticeContentSkill skill
    );
    void incrementSubmissionCountSinceCreationIfStudyPlanExists(
            String userId,
            PracticeContentSkill skill
    );
    LearnerStudyPlanResponseDto finalizeById(String id);
    LearnerStudyPlanRefreshAfterSubmissionResponseDto refreshAfterSubmission(
            String userId,
            PracticeContentSkill skill
    );}
