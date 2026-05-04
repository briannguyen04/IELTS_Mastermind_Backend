package com.ieltsmastermind.practice.studyplan.management.domain.entity;

import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTopicTag;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanFocusType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "learner_study_plan_strength_block",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_study_plan_strength_block_plan_rank",
                        columnNames = {
                                "learner_study_plan_id",
                                "strength_rank"
                        }
                )
        }
)
public class LearnerStudyPlanStrengthBlock {

    @Id
    @Column(name = "learner_study_plan_strength_block_id", nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @Column(name = "strength_rank", nullable = false)
    private Integer strengthRank = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "focus_type", nullable = false)
    private LearnerStudyPlanFocusType focusType;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type")
    private PracticeQuestionType questionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "topic_tag")
    private PracticeTopicTag topicTag;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence;

    @Column(name = "recommended_next_action", columnDefinition = "TEXT")
    private String recommendedNextAction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "learner_study_plan_id",
            referencedColumnName = "learner_study_plan_id",
            nullable = false
    )
    private LearnerStudyPlan learnerStudyPlan;
}
