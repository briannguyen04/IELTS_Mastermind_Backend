package com.ieltsmastermind.practice.analytics.management.persistence;

import com.ieltsmastermind.practice.analytics.management.domain.entity.LearnerWritingAnalyticsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LearnerWritingAnalyticsSnapshotRepository
        extends JpaRepository<LearnerWritingAnalyticsSnapshot, String> {

    Optional<LearnerWritingAnalyticsSnapshot> findTopByUser_UserIdOrderByVersionNumberDesc(
            String userId
    );
}