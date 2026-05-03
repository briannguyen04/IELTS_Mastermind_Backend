package com.ieltsmastermind.practice.analytics.management.persistence;

import com.ieltsmastermind.practice.analytics.management.domain.entity.LearnerAnalyticsSnapshot;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LearnerAnalyticsSnapshotRepository extends JpaRepository<LearnerAnalyticsSnapshot, String> {
    Optional<LearnerAnalyticsSnapshot> findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(
            String userId,
            PracticeContentSkill skill
    );
}