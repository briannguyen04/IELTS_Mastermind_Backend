package com.ieltsmastermind.ai.feedback.domain.dto;

import com.ieltsmastermind.ai.feedback.domain.entity.AIOutput;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StudyPlanAIResponse {
    private List<AIOutput> weaknesses;
    private List<AIOutput> strengths;
    private List<TaskOutput> tasks;
}
