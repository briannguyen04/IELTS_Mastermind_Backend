package com.ieltsmastermind.practice.studyplan.management.domain.dto;

import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTopicTag;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanFocusType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LearnerStudyPlanStrengthBlockResponseDto {

    private String id;
    private LearnerStudyPlanFocusType focusType;
    private PracticeQuestionType questionType;
    private PracticeTopicTag topicTag;
    private String description;
    private String explanation;
    private String evidence;
    private String recommendedNextAction;
}
