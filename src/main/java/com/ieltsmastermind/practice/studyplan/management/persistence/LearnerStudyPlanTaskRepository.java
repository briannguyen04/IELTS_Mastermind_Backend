package com.ieltsmastermind.practice.studyplan.management.persistence;

import com.ieltsmastermind.practice.studyplan.management.domain.entity.LearnerStudyPlanTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LearnerStudyPlanTaskRepository extends JpaRepository<LearnerStudyPlanTask, String> {

    @Query("SELECT t FROM LearnerStudyPlanTask t WHERE t.learnerStudyPlan.id = :id")
    List<LearnerStudyPlanTask> findTasksByStudyPlanId(String id);

}