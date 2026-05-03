package com.ieltsmastermind.practice.analytics.management.persistence;

import com.ieltsmastermind.practice.analytics.management.domain.entity.LearnerTrendSnapshot;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LearnerTrendSnapshotRepository extends JpaRepository<LearnerTrendSnapshot, String> {
    Optional<LearnerTrendSnapshot> findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(
            String userId,
            PracticeContentSkill skill
    );
}
