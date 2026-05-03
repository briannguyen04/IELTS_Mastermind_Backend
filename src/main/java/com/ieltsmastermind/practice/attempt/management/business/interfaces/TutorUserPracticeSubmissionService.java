package com.ieltsmastermind.practice.attempt.management.business.interfaces;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.attempt.management.domain.dto.TutorUserPracticeSubmissionResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.TutorUserPracticeSubmissionSetStatusRequestDto;

import java.util.List;

public interface TutorUserPracticeSubmissionService {

    TutorUserPracticeSubmissionResponseDto setTutorStatus(
            String tutorId,
            String userPracticeSubmissionId,
            TutorUserPracticeSubmissionSetStatusRequestDto request
    );
    List<TutorUserPracticeSubmissionResponseDto> getAllByTutorId(String tutorId, IncludeSpec includes);
    TutorUserPracticeSubmissionResponseDto getByTutorIdAndUserPracticeSubmissionId(
            String tutorId,
            String userPracticeSubmissionId,
            IncludeSpec includes
    );
}
