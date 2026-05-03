package com.ieltsmastermind.ai.feedback.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String submissionId;

    @Column(columnDefinition = "TEXT")
    private String summary;
}