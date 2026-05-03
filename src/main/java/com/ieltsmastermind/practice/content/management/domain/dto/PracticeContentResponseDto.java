package com.ieltsmastermind.practice.content.management.domain.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieltsmastermind.practice.content.management.domain.enums.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class PracticeContentResponseDto {

    private String id;
    private PracticeContentSkill skill;
    private String title;
    private String instructions;
    private JsonNode instructionsParsed;
    private PracticeTaskType task;
    private Set<PracticeQuestionType> questionTypeTags;
    private Set<PracticeTopicTag> topicTags;
    private String thumbnailUrl;
    private String audioUrl;
    private String transcript;
    private JsonNode transcriptParsed;
    private List<String> imageUrls;
    private String passage;
    private JsonNode passageParsed;
    private Integer durationMinutes;
    private Integer questionCount;
    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;
    private PracticeContentStatus status;
    private Long attemptCount;
}
