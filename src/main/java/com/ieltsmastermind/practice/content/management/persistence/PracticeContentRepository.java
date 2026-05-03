package com.ieltsmastermind.practice.content.management.persistence;

import com.ieltsmastermind.practice.content.management.domain.entity.PracticeContent;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PracticeContentRepository extends JpaRepository<PracticeContent, String> {
    @Modifying
    @Transactional
    @Query("""
        update PracticeContent pc
        set pc.attemptCount = coalesce(pc.attemptCount, 0) + 1
        where pc.id = :practiceContentId
    """)
    int incrementAttemptCount(@Param("practiceContentId") String practiceContentId);
}

