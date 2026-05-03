package com.ieltsmastermind.ai.feedback.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "question_feedback")
@Getter
@Setter
public class QuestionFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private Integer questionNumber;

    @ManyToOne
    @JoinColumn(name = "listening_feedback_id")
    private ListeningFeedback listeningFeedback;

    @ManyToOne
    @JoinColumn(name = "reading_feedback_id")
    private ReadingFeedback readingFeedback;

    @ManyToOne
    @JoinColumn(name = "writing_feedback_id")
    private WritingFeedback writingFeedback;

    @OneToOne(mappedBy = "questionFeedback", cascade = CascadeType.ALL)
    private Evidence evidence;

    @PrePersist
    @PreUpdate
    public void validate() {
        int count = 0;
        if (listeningFeedback != null) count++;
        if (readingFeedback != null) count++;
        if (writingFeedback != null) count++;

        if (count != 1) {
            throw new RuntimeException("QuestionFeedback must belong to exactly ONE skill");
        }
    }
}