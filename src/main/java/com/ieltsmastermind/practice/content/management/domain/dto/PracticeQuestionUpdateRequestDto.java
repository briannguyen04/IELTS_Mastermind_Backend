package com.ieltsmastermind.practice.content.management.domain.dto;

import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTopicTag;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PracticeQuestionUpdateRequestDto {

    private Integer orderIndex;
    private PracticeQuestionType type;
    private PracticeTopicTag topicTag;
    private List<String> correctAnswers;
}
