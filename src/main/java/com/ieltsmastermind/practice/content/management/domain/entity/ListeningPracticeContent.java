package com.ieltsmastermind.practice.content.management.domain.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "practice_listening_content")
@PrimaryKeyJoinColumn(name = "practice_content_id")
public class ListeningPracticeContent extends PracticeContent {

    @Column(name = "audio_url", nullable = false)
    private String audioUrl;


    @Column(name = "transcript", columnDefinition = "TEXT")
    private String transcript;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "transcript_parsed", columnDefinition = "JSON")
    private JsonNode transcriptParsed;

    public ListeningPracticeContent() {
        super(PracticeContentSkill.LISTENING);
    }
}