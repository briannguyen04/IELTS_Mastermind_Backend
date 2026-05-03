package com.ieltsmastermind.practice.attempt.management.persistence;


import com.ieltsmastermind.practice.attempt.management.domain.entity.TutorUserPracticeSubmission;
import com.ieltsmastermind.practice.attempt.management.domain.enums.TutorStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TutorUserPracticeSubmissionRepository
        extends JpaRepository<TutorUserPracticeSubmission, String> {

    Optional<TutorUserPracticeSubmission> findByTutor_UserIdAndUserPracticeSubmission_Id(
            String tutorId,
            String userPracticeSubmissionId
    );
    List<TutorUserPracticeSubmission> findAllByTutor_UserId(String tutorId);
    boolean existsByUserPracticeSubmission_IdAndTutorStatus(
            String userPracticeSubmissionId,
            TutorStatus tutorStatus
    );
    boolean existsByUserPracticeSubmission_IdAndTutor_UserIdNotAndTutorStatusIn(
            String userPracticeSubmissionId,
            String tutorId,
            Collection<TutorStatus> tutorStatuses
    );
}
