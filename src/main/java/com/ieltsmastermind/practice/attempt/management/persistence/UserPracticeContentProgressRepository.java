package com.ieltsmastermind.practice.attempt.management.persistence;

import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeContentProgress;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserPracticeContentProgressRepository
        extends JpaRepository<UserPracticeContentProgress, String> {

    Optional<UserPracticeContentProgress> findByUserIdAndPracticeContentId(
            String userId,
            String practiceContentId
    );
    List<UserPracticeContentProgress> findAllByUserId(String userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("""
        UPDATE UserPracticeContentProgress p
        SET p.attemptCount = COALESCE(p.attemptCount, 0) + 1
        WHERE p.userId = :userId
          AND p.practiceContentId = :practiceContentId
    """)
    int incrementAttemptCount(
            @Param("userId") String userId,
            @Param("practiceContentId") String practiceContentId
    );
}
