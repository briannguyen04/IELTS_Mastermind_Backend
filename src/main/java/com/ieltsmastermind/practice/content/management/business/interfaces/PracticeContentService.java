package com.ieltsmastermind.practice.content.management.business.interfaces;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.content.management.domain.dto.*;
import jakarta.transaction.Transactional;

import java.util.List;

public interface PracticeContentService {

    PracticeContentResponseDto create(PracticeContentCreateRequestDto request);

    List<PracticeContentResponseDto> getAll(IncludeSpec includes);

    PracticeContentResponseDto getById(String id, IncludeSpec includes);

    PracticeContentResponseDto update(String id, PracticeContentUpdateRequestDto request);

    void delete(String id);
}
