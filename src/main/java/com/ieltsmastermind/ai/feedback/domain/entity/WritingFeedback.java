package com.ieltsmastermind.ai.feedback.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "writing_feedback")
@Getter
@Setter
public class WritingFeedback extends BaseFeedback {

    @OneToMany(mappedBy = "writingFeedback", cascade = CascadeType.ALL)
    private List<QuestionFeedback> questions;
}
