package com.ieltsmastermind.practice.attempt.management.business.service;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.attempt.management.business.interfaces.SubmissionFeedbackService;
import com.ieltsmastermind.practice.attempt.management.domain.dto.SubmissionFeedbackAuthorResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.SubmissionFeedbackCreateRequestDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.SubmissionFeedbackResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.SubmissionFeedbackUpdateRequestDto;
import com.ieltsmastermind.practice.attempt.management.domain.entity.SubmissionFeedback;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeSubmission;
import com.ieltsmastermind.practice.attempt.management.persistence.SubmissionFeedbackRepository;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeSubmissionRepository;
import com.ieltsmastermind.user.management.domain.entity.User;
import com.ieltsmastermind.user.management.persistence.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubmissionFeedbackServiceImpl implements SubmissionFeedbackService {

    private final SubmissionFeedbackRepository submissionFeedbackRepository;
    private final UserPracticeSubmissionRepository userPracticeSubmissionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public SubmissionFeedbackResponseDto create(SubmissionFeedbackCreateRequestDto request) {

        UserPracticeSubmission submission = userPracticeSubmissionRepository
                .findById(request.getSubmissionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "UserPracticeSubmission not found: " + request.getSubmissionId()
                ));

        User user = userRepository
                .findById(request.getAuthorId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found: " + request.getAuthorId()
                ));

        SubmissionFeedback feedback = new SubmissionFeedback();
        feedback.setSubmission(submission);
        feedback.setAuthor(user);
        feedback.setAuthorType(request.getAuthorType());
        feedback.setFeedbackContent(request.getFeedbackContent());

        SubmissionFeedback saved = submissionFeedbackRepository.save(feedback);

        SubmissionFeedbackResponseDto responseDto = new SubmissionFeedbackResponseDto();
        responseDto.setId(saved.getId());

        return responseDto;
    }

    @Override
    public List<SubmissionFeedbackResponseDto> getAllBySubmissionId(String submissionId, IncludeSpec includes) {
        List<SubmissionFeedback> feedbackRows =
                submissionFeedbackRepository.findAllBySubmissionIdOrderByCreatedAtAsc(submissionId);

        List<SubmissionFeedbackResponseDto> result = new ArrayList<>();

        for (SubmissionFeedback row : feedbackRows) {
            SubmissionFeedbackResponseDto dto = new SubmissionFeedbackResponseDto();
            dto.setId(row.getId());
            applyIncludes(row, dto, includes);

            result.add(dto);
        }

        return result;
    }

    @Override
    @Transactional
    public SubmissionFeedbackResponseDto update(String id, SubmissionFeedbackUpdateRequestDto request) {
        SubmissionFeedback feedback = submissionFeedbackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Submission feedback not found with id: " + id));

        if (request.getAuthorType() != null) feedback.setAuthorType(request.getAuthorType());
        if (request.getFeedbackContent() != null) feedback.setFeedbackContent(request.getFeedbackContent());

        SubmissionFeedback saved = submissionFeedbackRepository.save(feedback);

        SubmissionFeedbackResponseDto dto = new SubmissionFeedbackResponseDto();
        dto.setId(saved.getId());

        return dto;
    }

    @Override
    @Transactional
    public void delete(String id) {
        SubmissionFeedback feedback = submissionFeedbackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Submission feedback not found with id: " + id));

        submissionFeedbackRepository.delete(feedback);
    }

    private void applyIncludes(SubmissionFeedback feedback,
                               SubmissionFeedbackResponseDto dto,
                               IncludeSpec includes) {

        if (includes.has("authortype")) dto.setAuthorType(feedback.getAuthorType());
        if (includes.has("feedbackcontent")) dto.setFeedbackContent(feedback.getFeedbackContent());
        if (includes.has("createdat")) dto.setCreatedAt(feedback.getCreatedAt());
        if (includes.has("updatedat")) dto.setUpdatedAt(feedback.getUpdatedAt());

        SubmissionFeedbackAuthorResponseDto authorDto = new SubmissionFeedbackAuthorResponseDto();

        if (includes.has("author.id")) authorDto.setId(feedback.getAuthor().getUserId());
        if (includes.has("author.firstname")) authorDto.setFirstname(feedback.getAuthor().getFirstname());
        if (includes.has("author.lastname")) authorDto.setLastname(feedback.getAuthor().getLastname());
        if (includes.has("author.avatarurl")) authorDto.setAvatarUrl(feedback.getAuthor().getAvatarUrl());

        dto.setAuthor(authorDto);
    }
}
