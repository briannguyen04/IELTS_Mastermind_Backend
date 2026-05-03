package com.ieltsmastermind.practice.content.management.persistence;

import com.ieltsmastermind.practice.content.management.domain.entity.PracticeContent;
import com.ieltsmastermind.practice.content.management.domain.entity.PracticeQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PracticeQuestionRepository extends JpaRepository<PracticeQuestion, String> {

    List<PracticeQuestion> findByPracticeContent_IdOrderByOrderIndexAsc(String practiceContentId);
}

