package com.ieltsmastermind.practice.studyplan.management.domain.entity;

import lombok.Getter;

@Getter
public class StudyPlanCreatedEvent {
    private final String studyPlanId;

    public StudyPlanCreatedEvent(String studyPlanId) {
        this.studyPlanId = studyPlanId;
    }

}