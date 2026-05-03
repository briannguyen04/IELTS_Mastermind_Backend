package com.ieltsmastermind.practice.attempt.management.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "user_practice_writing_answer",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_writing_answer_submission_order",
                        columnNames = {"user_practice_submission_id", "order_index"}
                )
        }
)
public class UserPracticeWritingAnswer {

    @Id
    @Column(name = "user_practice_writing_answer_id", nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Lob
    @Column(name = "essay_text", nullable = false, columnDefinition = "TEXT")
    private String essayText;

    @Column(name = "word_count", nullable = false)
    private Integer wordCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_practice_submission_id", nullable = false, updatable = false)
    private UserPracticeSubmission submission;

    @OneToMany(mappedBy = "writingAnswer", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("criterionName ASC, createdAt ASC")
    private List<UserPracticeWritingCriterionFeedback> criterionFeedbacks = new ArrayList<>();

    @OneToMany(mappedBy = "writingAnswer", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<UserPracticeWritingReview> writingReviews = new ArrayList<>();
}