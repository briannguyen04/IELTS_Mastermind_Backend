package com.ieltsmastermind.practice.content.management.business.service;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.content.management.business.interfaces.PracticeQuestionService;
import com.ieltsmastermind.practice.content.management.domain.dto.PracticeContentResponseDto;
import com.ieltsmastermind.practice.content.management.domain.dto.PracticeQuestionCreateRequestDto;
import com.ieltsmastermind.practice.content.management.domain.dto.PracticeQuestionResponseDto;
import com.ieltsmastermind.practice.content.management.domain.dto.PracticeQuestionUpdateRequestDto;
import com.ieltsmastermind.practice.content.management.domain.entity.PracticeContent;
import com.ieltsmastermind.practice.content.management.domain.entity.PracticeQuestion;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeQuestionType;
import com.ieltsmastermind.practice.content.management.persistence.PracticeContentRepository;
import com.ieltsmastermind.practice.content.management.persistence.PracticeQuestionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PracticeQuestionServiceImpl implements PracticeQuestionService {

    private final PracticeQuestionRepository practiceQuestionRepository;
    private final PracticeContentRepository practiceContentRepository;

    public PracticeQuestionServiceImpl(
            PracticeQuestionRepository practiceQuestionRepository,
            PracticeContentRepository practiceContentRepository
    ) {
        this.practiceQuestionRepository = practiceQuestionRepository;
        this.practiceContentRepository = practiceContentRepository;
    }

    @Override
    @Transactional
    public PracticeQuestionResponseDto create(String practiceContentId, PracticeQuestionCreateRequestDto request) {

        PracticeContent content = practiceContentRepository
                .findById(practiceContentId)
                .orElseThrow(() -> new RuntimeException("Practice content not found"));

        PracticeQuestion question = new PracticeQuestion();
        question.setPracticeContent(content);

        question.setOrderIndex(request.getOrderIndex());
        question.setType(request.getType());
        question.setTopicTag(request.getTopicTag());

        question.setCorrectAnswers(
                request.getCorrectAnswers() != null
                        ? new ArrayList<>(request.getCorrectAnswers())
                        : new ArrayList<>()
        );

        PracticeQuestion saved = practiceQuestionRepository.save(question);

        PracticeQuestionResponseDto dto = new PracticeQuestionResponseDto();
        dto.setId(saved.getId());

        return dto;
    }

    @Override
    public List<PracticeQuestionResponseDto> getAllByPracticeContentId(String practiceContentId, IncludeSpec includes) {

        practiceContentRepository.findById(practiceContentId)
                .orElseThrow(() -> new RuntimeException("Practice content not found"));

        List<PracticeQuestion> questions =
                practiceQuestionRepository.findByPracticeContent_IdOrderByOrderIndexAsc(practiceContentId);

        List<PracticeQuestionResponseDto> result = new ArrayList<>();
        for (PracticeQuestion q : questions) {
            PracticeQuestionResponseDto dto = new PracticeQuestionResponseDto();
            dto.setId(q.getId());
            dto.setPracticeContentId(practiceContentId);
            applyIncludes(q, dto, includes);
            result.add(dto);
        }

        return result;
    }

    @Override
    @Transactional
    public PracticeQuestionResponseDto update(String id, PracticeQuestionUpdateRequestDto request) {
        PracticeQuestion question = practiceQuestionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Practice question not found with id: " + id));

        if (request.getOrderIndex() != null) question.setOrderIndex(request.getOrderIndex());
        if (request.getType() != null) question.setType(request.getType());
        if (request.getTopicTag() != null) question.setTopicTag(request.getTopicTag());
        if (request.getCorrectAnswers() != null) question.setCorrectAnswers(request.getCorrectAnswers());

        PracticeQuestion saved = practiceQuestionRepository.save(question);

        PracticeQuestionResponseDto dto = new PracticeQuestionResponseDto();
        dto.setId(saved.getId());

        return dto;
    }

    @Override
    @Transactional
    public void delete(String id) {
        PracticeQuestion question = practiceQuestionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Practice question not found with id: " + id));

        practiceQuestionRepository.delete(question);
    }

    private void applyIncludes(PracticeQuestion content, PracticeQuestionResponseDto dto, IncludeSpec includes) {
        if (includes.has("orderindex")) dto.setOrderIndex(content.getOrderIndex());
        if (includes.has("type")) dto.setType(content.getType());
        if (includes.has("topictag")) dto.setTopicTag(content.getTopicTag());
        if (includes.has("correctanswers")) dto.setCorrectAnswers(content.getCorrectAnswers());
    }
}
