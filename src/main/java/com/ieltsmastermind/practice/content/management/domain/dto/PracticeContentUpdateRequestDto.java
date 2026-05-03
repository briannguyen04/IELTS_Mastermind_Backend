package com.ieltsmastermind.practice.content.management.domain.dto;

import com.ieltsmastermind.practice.content.management.domain.enums.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
public class PracticeContentUpdateRequestDto {

    private PracticeContentSkill skill;
    private String title;
    private String instructions;
    private PracticeTaskType task;
    private Set<PracticeQuestionType> questionTypeTags;
    private Set<PracticeTopicTag> topicTags;
    private String thumbnailUrl;
    private String audioUrl;
    private String transcript;
    private List<String> imageUrls;
    private String passage;
    private Integer durationMinutes;
    private Integer questionCount;
    private PracticeContentStatus status;
}
