package com.ieltsmastermind.practice.analytics.management.business.interfaces;

import com.ieltsmastermind.practice.analytics.management.domain.dto.SubmissionAnalyticsCreateRequestDto;
import com.ieltsmastermind.practice.analytics.management.domain.dto.SubmissionAnalyticsResponseDto;
import com.ieltsmastermind.practice.analytics.management.domain.dto.SubmissionAnalyticsSkillCountResponseDto;
import com.ieltsmastermind.practice.analytics.management.domain.entity.SubmissionAnalytics;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTopicTag;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanFocusType;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanTargetMetric;

public interface SubmissionAnalyticsService {

    SubmissionAnalyticsResponseDto create(SubmissionAnalyticsCreateRequestDto request);

    SubmissionAnalytics createSnapshot(
            String learnerId,
            PracticeContentSkill skill
    );

    Double getCurrentValue(
            LearnerStudyPlanFocusType focusType,
            LearnerStudyPlanTargetMetric targetMetric,
            String userId,
            PracticeContentSkill skill,
            PracticeQuestionType questionType,
            PracticeTopicTag topicTag
    );
    SubmissionAnalyticsSkillCountResponseDto getAnalyticsSubmissionCountsByUserId(String userId);
}