package com.ieltsmastermind.practice.analytics.management.persistence;

import com.ieltsmastermind.practice.analytics.management.domain.entity.SubmissionAnalytics;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubmissionAnalyticsRepository extends JpaRepository<SubmissionAnalytics, String> {

    Optional<SubmissionAnalytics> findTopByUser_UserIdAndSkillOrderByVersionNumberDesc(
            String userId,
            PracticeContentSkill skill
    );
}