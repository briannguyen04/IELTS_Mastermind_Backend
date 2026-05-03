package com.ieltsmastermind.practice.analytics.management.business.interfaces;

import com.ieltsmastermind.practice.analytics.management.domain.dto.LearnerAnalyticsSnapshotCreateRequestDto;
import com.ieltsmastermind.practice.analytics.management.domain.dto.LearnerAnalyticsSnapshotResponseDto;
import com.ieltsmastermind.practice.analytics.management.domain.entity.LearnerAnalyticsSnapshot;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTopicTag;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanFocusType;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanTargetMetric;

public interface LearnerAnalyticsSnapshotService {
    LearnerAnalyticsSnapshotResponseDto create(LearnerAnalyticsSnapshotCreateRequestDto request);
    LearnerAnalyticsSnapshot createSnapshot(String learnerId, PracticeContentSkill skill);
    Double getCurrentValue(
            LearnerStudyPlanFocusType focusType,
            LearnerStudyPlanTargetMetric targetMetric,
            String userId,
            PracticeContentSkill skill,
            PracticeQuestionType questionType,
            PracticeTopicTag topicTag
    );
}
