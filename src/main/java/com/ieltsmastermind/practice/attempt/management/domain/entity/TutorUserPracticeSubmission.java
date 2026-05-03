package com.ieltsmastermind.practice.attempt.management.domain.entity;

import com.ieltsmastermind.practice.attempt.management.domain.enums.TutorStatus;
import com.ieltsmastermind.user.management.domain.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "tutor_user_practice_submission",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_tutor_user_practice_submission_tutor_submission",
                        columnNames = {"tutor_id", "user_practice_submission_id"}
                )
        }
)
public class TutorUserPracticeSubmission {

    @Id
    @Column(name = "tutor_user_practice_submission_id", nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @Enumerated(EnumType.STRING)
    @Column(name = "tutor_status", nullable = false)
    private TutorStatus tutorStatus = TutorStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "tutor_id",
            referencedColumnName = "user_id",
            nullable = false
    )
    private User tutor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_practice_submission_id",
            referencedColumnName = "user_practice_submission_id",
            nullable = false
    )
    private UserPracticeSubmission userPracticeSubmission;
}
