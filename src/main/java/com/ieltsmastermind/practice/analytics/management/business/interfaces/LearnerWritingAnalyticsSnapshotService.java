package com.ieltsmastermind.practice.analytics.management.business.interfaces;

import com.ieltsmastermind.practice.analytics.management.domain.dto.LearnerWritingAnalyticsSnapshotCreateRequestDto;
import com.ieltsmastermind.practice.analytics.management.domain.dto.LearnerWritingAnalyticsSnapshotResponseDto;
import com.ieltsmastermind.practice.analytics.management.domain.entity.LearnerWritingAnalyticsSnapshot;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTopicTag;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanFocusType;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanTargetMetric;

public interface LearnerWritingAnalyticsSnapshotService {

    LearnerWritingAnalyticsSnapshotResponseDto create(
            LearnerWritingAnalyticsSnapshotCreateRequestDto request
    );
    LearnerWritingAnalyticsSnapshot createSnapshot(String learnerId);
    Double getCurrentValue(
            LearnerStudyPlanFocusType focusType,
            LearnerStudyPlanTargetMetric targetMetric,
            String userId,
            PracticeQuestionType questionType,
            PracticeTopicTag topicTag
    );
}
