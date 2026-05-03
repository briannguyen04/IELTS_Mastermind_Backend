package com.ieltsmastermind.practice.studyplan.management.domain.entity;

import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeTopicTag;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanFocusType;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanTargetMetric;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanTaskDirection;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanTaskStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "learner_study_plan_task",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_study_plan_task_plan_priority",
                        columnNames = {
                                "learner_study_plan_id",
                                "priority_rank"
                        }
                )
        }
)
public class LearnerStudyPlanTask {

    @Id
    @Column(name = "learner_study_plan_task_id", nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @Column(name = "priority_rank", nullable = false)
    private Integer priorityRank = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "focus_type", nullable = false)
    private LearnerStudyPlanFocusType focusType;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type")
    private PracticeQuestionType questionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "topic_tag")
    private PracticeTopicTag topicTag;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_metric", nullable = false)
    private LearnerStudyPlanTargetMetric targetMetric;

    @Column(name = "current_value", nullable = false)
    private Double currentValue = 0.0;

    @Column(name = "target_value", nullable = false)
    private Double targetValue = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private LearnerStudyPlanTaskDirection direction = LearnerStudyPlanTaskDirection.INCREASE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LearnerStudyPlanTaskStatus status = LearnerStudyPlanTaskStatus.ACTIVE;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "learner_study_plan_id",
            referencedColumnName = "learner_study_plan_id",
            nullable = false
    )
    private LearnerStudyPlan learnerStudyPlan;
}
