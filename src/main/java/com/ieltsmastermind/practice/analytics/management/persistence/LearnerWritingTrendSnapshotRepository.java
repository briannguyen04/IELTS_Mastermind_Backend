package com.ieltsmastermind.practice.analytics.management.persistence;

import com.ieltsmastermind.practice.analytics.management.domain.entity.LearnerWritingTrendSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LearnerWritingTrendSnapshotRepository
        extends JpaRepository<LearnerWritingTrendSnapshot, String> {

    Optional<LearnerWritingTrendSnapshot> findTopByUser_UserIdOrderByVersionNumberDesc(
            String userId
    );
}
