package com.ieltsmastermind.practice.attempt.management.business.service;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.attempt.management.business.interfaces.UserPracticeWritingAnswerService;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeWritingAnswerCreateRequestDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeWritingAnswerResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeSubmission;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeWritingAnswer;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeSubmissionRepository;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeWritingAnswerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserPracticeWritingAnswerServiceImpl implements UserPracticeWritingAnswerService {

    private final UserPracticeSubmissionRepository submissionRepository;
    private final UserPracticeWritingAnswerRepository writingAnswerRepository;

    @Override
    @Transactional
    public UserPracticeWritingAnswerResponseDto create(
            UserPracticeWritingAnswerCreateRequestDto request
    ) {
        UserPracticeSubmission submission = submissionRepository
                .findById(request.getUserPracticeSubmissionId())
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        UserPracticeWritingAnswer writingAnswer = new UserPracticeWritingAnswer();
        writingAnswer.setSubmission(submission);
        writingAnswer.setOrderIndex(request.getOrderIndex());
        writingAnswer.setEssayText(request.getEssayText());
        writingAnswer.setWordCount(countWords(request.getEssayText()));

        UserPracticeWritingAnswer saved = writingAnswerRepository.save(writingAnswer);

        UserPracticeWritingAnswerResponseDto responseDto =
                new UserPracticeWritingAnswerResponseDto();
        responseDto.setId(saved.getId());

        return responseDto;
    }

    @Override
    public List<UserPracticeWritingAnswerResponseDto> getAllBySubmissionId(
            String submissionId,
            IncludeSpec includes
    ) {
        List<UserPracticeWritingAnswer> answerRows =
                writingAnswerRepository.findAllBySubmission_IdOrderByOrderIndexAsc(submissionId);

        List<UserPracticeWritingAnswerResponseDto> result = new ArrayList<>();

        for (UserPracticeWritingAnswer row : answerRows) {
            UserPracticeWritingAnswerResponseDto dto = new UserPracticeWritingAnswerResponseDto();
            dto.setId(row.getId());
            applyIncludes(row, dto, includes);

            result.add(dto);
        }

        return result;
    }

    @Override
    public UserPracticeWritingAnswerResponseDto getById(String id, IncludeSpec includes) {

        UserPracticeWritingAnswer answer = writingAnswerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Writing answer not found"));

        UserPracticeWritingAnswerResponseDto dto = new UserPracticeWritingAnswerResponseDto();
        dto.setId(answer.getId());
        applyIncludes(answer, dto, includes);

        return dto;
    }

    private void applyIncludes(UserPracticeWritingAnswer answer,
                               UserPracticeWritingAnswerResponseDto dto,
                               IncludeSpec includes) {

        if (includes.has("submissionid")) dto.setSubmissionId(answer.getSubmission().getId());
        if (includes.has("orderindex")) dto.setOrderIndex(answer.getOrderIndex());
        if (includes.has("essaytext")) dto.setEssayText(answer.getEssayText());
        if (includes.has("wordcount")) dto.setWordCount(answer.getWordCount());
    }

    private int countWords(String essayText) {
        if (essayText == null || essayText.trim().isEmpty()) {
            return 0;
        }
        return essayText.trim().split("\\s+").length;
    }
}
