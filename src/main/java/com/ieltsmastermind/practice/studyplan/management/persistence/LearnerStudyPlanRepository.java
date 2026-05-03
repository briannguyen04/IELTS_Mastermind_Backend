package com.ieltsmastermind.practice.studyplan.management.persistence;

import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;
import com.ieltsmastermind.practice.studyplan.management.domain.entity.LearnerStudyPlan;
import com.ieltsmastermind.practice.studyplan.management.domain.entity.LearnerStudyPlanStrengthBlock;
import com.ieltsmastermind.practice.studyplan.management.domain.entity.LearnerStudyPlanTask;
import com.ieltsmastermind.practice.studyplan.management.domain.entity.LearnerStudyPlanWeaknessBlock;
import com.ieltsmastermind.practice.studyplan.management.domain.enums.LearnerStudyPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LearnerStudyPlanRepository extends JpaRepository<LearnerStudyPlan, String> {

    Optional<LearnerStudyPlan> findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(
            String userId,
            PracticeContentSkill skill
    );
    Optional<LearnerStudyPlan> findTopByUser_UserIdAndSkillAndStatusOrderByVersionNumberDesc(
            String userId,
            PracticeContentSkill skill,
            LearnerStudyPlanStatus status
    );
    boolean existsByUser_UserIdAndSkillAndStatus(
            String userId,
            PracticeContentSkill skill,
            LearnerStudyPlanStatus status
    );
}
