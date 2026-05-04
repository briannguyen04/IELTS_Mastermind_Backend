package com.ieltsmastermind.practice.content.management.domain.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeContentProgress;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeSubmission;
import com.ieltsmastermind.practice.content.management.domain.enums.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "practice_content")
@Inheritance(strategy = InheritanceType.JOINED)
public class PracticeContent {

    @Id
    @Column(name = "practice_content_id", nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @Enumerated(EnumType.STRING)
    @Column(name = "skill", nullable = false, updatable = false)
    private PracticeContentSkill skill;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "instructions_parsed", columnDefinition = "JSON")
    private JsonNode instructionsParsed;

    @Enumerated(EnumType.STRING)
    @Column(name = "task", nullable = false)
    private PracticeTaskType task;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "practice_content_question_type_tag",
            joinColumns = @JoinColumn(name = "practice_content_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "tag")
    private Set<PracticeQuestionType> questionTypeTags = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "practice_content_topic_tag",
            joinColumns = @JoinColumn(name = "practice_content_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "tag")
    private Set<PracticeTopicTag> topicTags = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "practice_content_images",
            joinColumns = @JoinColumn(name = "practice_content_id")
    )
    @Column(name = "image_url")
    private List<String> imageUrls = new ArrayList<>();

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "question_count")
    private Integer questionCount;

    @Column(name = "updated_on", nullable = false)
    private LocalDateTime updatedOn;

    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PracticeContentStatus status;

    @Column(name = "attempt_count", nullable = false)
    private Long attemptCount = 0L;

    @OneToMany(mappedBy = "practiceContent", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<PracticeQuestion> questions = new ArrayList<>();

    @OneToMany(mappedBy = "practiceContent")
    private List<UserPracticeSubmission> submissions = new ArrayList<>();

    @OneToMany(mappedBy = "practiceContent", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<UserPracticeContentProgress> practiceContentProgresses = new ArrayList<>();

    protected PracticeContent(PracticeContentSkill skill) {
        this.skill = skill;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdOn = now;
        this.updatedOn = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedOn = LocalDateTime.now();
    }
}
