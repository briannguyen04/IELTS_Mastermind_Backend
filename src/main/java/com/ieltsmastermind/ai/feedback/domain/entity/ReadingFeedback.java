package com.ieltsmastermind.ai.feedback.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "reading_feedback")
@Getter
@Setter
public class ReadingFeedback extends BaseFeedback {

    @OneToMany(mappedBy = "readingFeedback", cascade = CascadeType.ALL)
    private List<QuestionFeedback> questions;
}
