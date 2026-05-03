package com.ieltsmastermind.practice.studyplan.management.domain.dto;

import com.ieltsmastermind.practice.analytics.management.domain.enums.AnalyticsConclusionLabel;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTopicTag;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanFocusType;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanWeaknessBlockStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LearnerStudyPlanWeaknessBlockResponseDto {

    private String id;
    private LearnerStudyPlanFocusType focusType;
    private PracticeQuestionType questionType;
    private PracticeTopicTag topicTag;
    private LearnerStudyPlanWeaknessBlockStatus status;
    private AnalyticsConclusionLabel analyticsConclusionLabel;
    private String description;
    private String explanation;
    private String evidence;
    private String recommendedNextAction;
}
