package com.ieltsmastermind.practice.content.management.domain.entity;

import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTopicTag;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "practice_question",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_practice_question_content_order",
                        columnNames = {"practice_content_id", "order_index"}
                )
        }
)
public class PracticeQuestion {

    @Id
    @Column(name = "practice_question_id", nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PracticeQuestionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "topic_tag", nullable = false)
    private PracticeTopicTag topicTag;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "practice_question_answer",
            joinColumns = @JoinColumn(name = "practice_question_id")
    )
    @Column(name = "answer_value", nullable = false)
    @OrderColumn(name = "answer_index")
    private List<String> correctAnswers = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "practice_content_id", nullable = false)
    private PracticeContent practiceContent;
}
