package com.ieltsmastermind.practice.studyplan.management.domain.dto;

import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTopicTag;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanFocusType;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanTargetMetric;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanTaskDirection;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanTaskStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LearnerStudyPlanTaskResponseDto {

    private String id;
    private LearnerStudyPlanFocusType focusType;
    private PracticeQuestionType questionType;
    private PracticeTopicTag topicTag;
    private LearnerStudyPlanTargetMetric targetMetric;
    private Double currentValue;
    private Double targetValue;
    private LearnerStudyPlanTaskDirection direction;
    private LearnerStudyPlanTaskStatus status;
    private String description;
}
