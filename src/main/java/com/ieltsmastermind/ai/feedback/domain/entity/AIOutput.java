package com.ieltsmastermind.ai.feedback.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AIOutput {
    private String id;
    private String description;
    private String explanation;
    private String evidence;
    private String recommendation;
}