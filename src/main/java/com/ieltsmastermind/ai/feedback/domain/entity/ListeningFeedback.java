package com.ieltsmastermind.ai.feedback.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "listening_feedback")
@Getter
@Setter
public class ListeningFeedback extends BaseFeedback {

    @OneToMany(mappedBy = "listeningFeedback", cascade = CascadeType.ALL)
    private List<QuestionFeedback> questions;
}