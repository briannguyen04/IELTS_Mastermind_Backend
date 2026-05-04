package com.ieltsmastermind.user.management.domain.entity;

import com.ieltsmastermind.practice.analytics.management.domain.entity.SubmissionAnalytics;
import com.ieltsmastermind.practice.attempt.management.domain.entity.*;
import com.ieltsmastermind.practice.studyplan.management.domain.entity.LearnerStudyPlan;
import com.ieltsmastermind.user.management.domain.enums.AuthProvider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user")
public class User {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private String userId = UUID.randomUUID().toString();

    @Column(unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    @Column(name = "provider_id", length = 100)
    private String providerId;

    @Column(unique = true)
    private String phoneNumber;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "is_active")
    private Boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "role")
    private String role;

    @Column(name = "firstname")
    private String firstname;

    @Column(name = "lastname")
    private String lastname;

    @Column(name = "gender")
    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "country")
    private String country;

    @Column(name = "timezone")
    private String timezone;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Deprecated(since = "2026-04")
    @Column(name = "target_band")
    private Integer targetBand;

    @Column(name = "target_listening_band", nullable = false)
    private Double targetListeningBand = 0.0;

    @Column(name = "target_reading_band", nullable = false)
    private Double targetReadingBand = 0.0;

    @Column(name = "target_writing_band", nullable = false)
    private Double targetWritingBand = 0.0;

    @Column(name = "target_speaking_band", nullable = false)
    private Double targetSpeakingBand = 0.0;

    @Column(name = "exam_date")
    private LocalDateTime examDate;

    @OneToMany(mappedBy = "user")
    private List<UserPracticeSubmission> practiceSubmissions = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<UserPracticeContentProgress> practiceContentProgresses = new ArrayList<>();

    @OneToMany(mappedBy = "author")
    private List<SubmissionFeedback> submissionFeedbacks = new ArrayList<>();

    @OneToMany(mappedBy = "tutor", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<TutorUserPracticeSubmission> tutorUserPracticeSubmissions = new ArrayList<>();

    @OneToMany(mappedBy = "reviewedByUser", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<UserPracticeWritingCriterionFeedback> writingCriterionFeedbacks = new ArrayList<>();

    @OneToMany(mappedBy = "reviewedByUser", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<UserPracticeWritingReview> writingReviews = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @OrderBy("createdAt DESC")
    private List<LearnerStudyPlan> learnerStudyPlans = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @OrderBy("calculatedAt DESC")
    private List<SubmissionAnalytics> submissionAnalytics = new ArrayList<>();
}
