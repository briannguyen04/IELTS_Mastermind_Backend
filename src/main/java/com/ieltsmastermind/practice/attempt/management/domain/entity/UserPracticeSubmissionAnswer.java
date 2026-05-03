package com.ieltsmastermind.practice.attempt.management.domain.entity;

import com.ieltsmastermind.practice.attempt.management.domain.enums.Result;
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
@NoArgsConstructor
@Entity
@Table(
        name = "user_practice_submission_answer",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_submission_answer_submission_order",
                        columnNames = {"user_practice_submission_id", "order_index"}
                )
        }
)
public class UserPracticeSubmissionAnswer {

    @Id
    @Column(name = "user_practice_submission_answer_id", nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @ElementCollection
    @CollectionTable(
            name = "user_practice_submission_answer_value",
            joinColumns = @JoinColumn(name = "user_practice_submission_answer_id")
    )
    @Column(name = "answer_value", nullable = false)
    @OrderColumn(name = "answer_index")
    private List<String> answers = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false)
    private Result result = Result.SKIPPED;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_practice_submission_id", nullable = false)
    private UserPracticeSubmission submission;
}