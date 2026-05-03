package com.ieltsmastermind.practice.content.management.business.interfaces;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.content.management.domain.dto.PracticeQuestionCreateRequestDto;
import com.ieltsmastermind.practice.content.management.domain.dto.PracticeQuestionResponseDto;
import com.ieltsmastermind.practice.content.management.domain.dto.PracticeQuestionUpdateRequestDto;

import java.util.List;

public interface PracticeQuestionService {

    PracticeQuestionResponseDto create(String practiceContentId, PracticeQuestionCreateRequestDto request);

    List<PracticeQuestionResponseDto> getAllByPracticeContentId(String practiceContentId, IncludeSpec includes);

    PracticeQuestionResponseDto update(String id, PracticeQuestionUpdateRequestDto request);

    void delete(String id);
}
