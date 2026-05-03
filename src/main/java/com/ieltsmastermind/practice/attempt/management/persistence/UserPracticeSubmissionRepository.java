package com.ieltsmastermind.practice.attempt.management.persistence;

import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeSubmission;
import com.ieltsmastermind.practice.attempt.management.domain.enums.TutorStatus;
import com.ieltsmastermind.practice.attempt.management.persistence.projection.UserPracticeSubmissionSkillCountProjection;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserPracticeSubmissionRepository extends JpaRepository<UserPracticeSubmission, String> {

    List<UserPracticeSubmission> findAllByUserIdOrderBySubmittedAtDesc(String userId);
    List<UserPracticeSubmission> findAllByOrderBySubmittedAtDesc();
    List<UserPracticeSubmission> findByIsTutorReviewRequestedTrueOrderBySubmittedAtDesc();
    List<UserPracticeSubmission> findAllByUserIdAndSubmittedAtBetweenOrderBySubmittedAtDesc(
            String userId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    @Query("""
    SELECT s FROM UserPracticeSubmission s
    JOIN FETCH s.practiceContent
    WHERE s.id = :id
""")
    Optional<UserPracticeSubmission> findWithContent(@Param("id") String id);

    List<UserPracticeSubmission> findByUserIdAndPracticeContent_SkillOrderBySubmittedAtAsc(
            String userId,
            PracticeContentSkill skill
    );

    @Query("""
    SELECT pc.skill AS skill, COUNT(ups.id) AS submissionCount
    FROM UserPracticeSubmission ups
    JOIN ups.practiceContent pc
    WHERE ups.userId = :userId
      AND (
          pc.skill <> :writingSkill
          OR ups.tutorStatus = :completedTutorStatus
      )
    GROUP BY pc.skill
""")
    List<UserPracticeSubmissionSkillCountProjection> countSubmissionsBySkillAndUserId(
            @Param("userId") String userId,
            @Param("writingSkill") PracticeContentSkill writingSkill,
            @Param("completedTutorStatus") TutorStatus completedTutorStatus
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM UserPracticeSubmission s WHERE s.id = :id")
    Optional<UserPracticeSubmission> findByIdForUpdate(@Param("id") String id);
}
