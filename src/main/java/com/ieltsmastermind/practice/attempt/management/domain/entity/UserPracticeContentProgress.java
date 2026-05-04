package com.ieltsmastermind.practice.attempt.management.domain.entity;

import com.ieltsmastermind.practice.content.management.domain.entity.PracticeContent;
import com.ieltsmastermind.user.management.domain.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "user_practice_content_progress",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_practice_content_progress_user_content",
                        columnNames = {"user_id", "practice_content_id"}
                )
        }
)
public class UserPracticeContentProgress {

    @Id
    @Column(name = "user_practice_content_progress_id", nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @Column(name = "user_id")
    private String userId;

    @Column(name = "practice_content_id", nullable = false)
    private String practiceContentId;

    @Column(name = "is_bookmarked", nullable = false)
    private Boolean isBookmarked = false;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "practice_content_id",
            referencedColumnName = "practice_content_id",
            insertable = false,
            updatable = false
    )
    private PracticeContent practiceContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            referencedColumnName = "user_id",
            insertable = false,
            updatable = false
    )
    private User user;
}