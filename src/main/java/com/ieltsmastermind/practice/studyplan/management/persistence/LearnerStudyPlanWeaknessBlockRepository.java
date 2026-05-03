package com.ieltsmastermind.practice.studyplan.management.persistence;

import com.ieltsmastermind.practice.studyplan.management.domain.entity.LearnerStudyPlanWeaknessBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LearnerStudyPlanWeaknessBlockRepository
        extends JpaRepository<LearnerStudyPlanWeaknessBlock, String> {

    @Query("SELECT w FROM LearnerStudyPlanWeaknessBlock w WHERE w.learnerStudyPlan.id = :id")
    List<LearnerStudyPlanWeaknessBlock> findWeaknessBlocksByStudyPlanId(String id);
}