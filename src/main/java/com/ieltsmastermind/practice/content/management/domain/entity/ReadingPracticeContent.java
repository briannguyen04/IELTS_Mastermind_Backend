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
@Table(name = "practice_reading_content")
@PrimaryKeyJoinColumn(name = "practice_content_id")
public class ReadingPracticeContent extends PracticeContent {

    @Column(name = "passage", nullable = false, columnDefinition = "TEXT")
    private String passage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "passage_parsed", columnDefinition = "JSON")
    private JsonNode passageParsed;

    public ReadingPracticeContent() {
        super(PracticeContentSkill.READING);
    }
}