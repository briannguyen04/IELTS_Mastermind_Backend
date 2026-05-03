package com.ieltsmastermind.practice.studyplan.management.persistence;

import com.ieltsmastermind.practice.studyplan.management.domain.entity.LearnerStudyPlanStrengthBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LearnerStudyPlanStrengthBlockRepository
        extends JpaRepository<LearnerStudyPlanStrengthBlock, String> {

    @Query("SELECT s FROM LearnerStudyPlanStrengthBlock s WHERE s.learnerStudyPlan.id = :id")
    List<LearnerStudyPlanStrengthBlock> findStrengthBlocksByStudyPlanId(String id);
}
