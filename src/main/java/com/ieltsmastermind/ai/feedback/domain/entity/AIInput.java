package com.ieltsmastermind.ai.feedback.domain.entity;

import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTopicTag;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanFocusType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AIInput {
    private String id;
    private String type; // WEAKNESS / STRENGTH
    private LearnerStudyPlanFocusType focusType;
    private PracticeQuestionType questionType;
    private PracticeTopicTag topicTag;
    private double correctRate;
}