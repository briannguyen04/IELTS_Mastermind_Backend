package com.ieltsmastermind.practice.studyplan.management.business.interfaces;

import com.ieltsmastermind.practice.studyplan.management.domain.dto.LearnerStudyPlanResponseDto;
import com.ieltsmastermind.practice.studyplan.management.domain.dto.LearnerWritingStudyPlanCreateRequestDto;

public interface LearnerWritingStudyPlanService {

    LearnerStudyPlanResponseDto createForLearner(String learnerId);
    void refreshStudyPlanIfStudyPlanExists(String userId);
}
