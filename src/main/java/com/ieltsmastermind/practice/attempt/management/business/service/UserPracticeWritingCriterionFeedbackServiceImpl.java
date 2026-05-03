package com.ieltsmastermind.practice.attempt.management.business.service;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.attempt.management.business.interfaces.UserPracticeWritingCriterionFeedbackService;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeWritingCriterionFeedbackCreateRequestDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeWritingCriterionFeedbackResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeWritingCriterionFeedbackUpdateRequestDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeWritingCriterionFeedbackUserResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeWritingAnswer;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeWritingCriterionFeedback;
import com.ieltsmastermind.practice.attempt.management.domain.enums.FeedbackAuthorType;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeWritingAnswerRepository;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeWritingCriterionFeedbackRepository;
import com.ieltsmastermind.user.management.domain.entity.User;
import com.ieltsmastermind.user.management.persistence.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserPracticeWritingCriterionFeedbackServiceImpl implements UserPracticeWritingCriterionFeedbackService {

    private final UserPracticeWritingCriterionFeedbackRepository
            userPracticeWritingCriterionFeedbackRepository;

    private final UserPracticeWritingAnswerRepository userPracticeWritingAnswerRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserPracticeWritingCriterionFeedbackResponseDto create(
            UserPracticeWritingCriterionFeedbackCreateRequestDto request
    ) {
        UserPracticeWritingAnswer writingAnswer = userPracticeWritingAnswerRepository
                .findById(request.getUserPracticeWritingAnswerId())
                .orElseThrow(() -> new RuntimeException("Writing answer not found"));

        User reviewedByUser = null;
        if (request.getReviewedByUserId() != null && !request.getReviewedByUserId().isBlank()) {
            reviewedByUser = userRepository
                    .findById(request.getReviewedByUserId())
                    .orElseThrow(() -> new RuntimeException("Reviewed by user not found"));
        }
        UserPracticeWritingCriterionFeedback feedback = new UserPracticeWritingCriterionFeedback();
        feedback.setAuthorType(request.getAuthorType());
        feedback.setCriterionName(request.getCriterionName());
        feedback.setFeedbackType(request.getFeedbackType());
        feedback.setLabel(request.getLabel());
        feedback.setDescription(request.getDescription());
        feedback.setExplanation(request.getExplanation());
        feedback.setEvidenceSentences(
                request.getEvidenceSentences() != null
                        ? request.getEvidenceSentences()
                        : new ArrayList<>()
        );
        feedback.setRecommendedActionDescription(request.getRecommendedActionDescription());
        feedback.setRecommendedActionExplanation(request.getRecommendedActionExplanation());
        feedback.setWritingAnswer(writingAnswer);
        feedback.setReviewedByUser(reviewedByUser);

        UserPracticeWritingCriterionFeedback saved =
                userPracticeWritingCriterionFeedbackRepository.save(feedback);

        UserPracticeWritingCriterionFeedbackResponseDto responseDto =
                new UserPracticeWritingCriterionFeedbackResponseDto();
        responseDto.setId(saved.getId());

        return responseDto;
    }

    @Override
    @Transactional
    public List<UserPracticeWritingCriterionFeedbackResponseDto> getAllBySubmissionId(
            String submissionId,
            IncludeSpec includes
    ) {
        List<UserPracticeWritingCriterionFeedback> feedbacks =
                userPracticeWritingCriterionFeedbackRepository
                        .findAllBySubmissionIdOrderByCreatedAtAsc(submissionId);

        List<UserPracticeWritingCriterionFeedbackResponseDto> result = new ArrayList<>();

        for (UserPracticeWritingCriterionFeedback feedback : feedbacks) {
            UserPracticeWritingCriterionFeedbackResponseDto dto =
                    new UserPracticeWritingCriterionFeedbackResponseDto();

            dto.setId(feedback.getId());

            applyIncludes(feedback, dto, includes);

            result.add(dto);
        }

        return result;
    }

    @Override
    @Transactional
    public UserPracticeWritingCriterionFeedbackResponseDto update(
            String id,
            UserPracticeWritingCriterionFeedbackUpdateRequestDto request
    ) {
        UserPracticeWritingCriterionFeedback feedback =
                userPracticeWritingCriterionFeedbackRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException(
                                "User practice writing criterion feedback not found with id: " + id
                        ));

        if (request.getLabel() != null) feedback.setLabel(request.getLabel());
        if (request.getDescription() != null) feedback.setDescription(request.getDescription());
        if (request.getExplanation() != null) feedback.setExplanation(request.getExplanation());
        if (request.getEvidenceSentences() != null) feedback.setEvidenceSentences(request.getEvidenceSentences());
        if (request.getRecommendedActionDescription() != null) feedback.setRecommendedActionDescription(request.getRecommendedActionDescription());
        if (request.getRecommendedActionExplanation() != null) feedback.setRecommendedActionExplanation(request.getRecommendedActionExplanation());

        feedback.setAuthorType(FeedbackAuthorType.TUTOR);

        UserPracticeWritingCriterionFeedback saved =
                userPracticeWritingCriterionFeedbackRepository.save(feedback);

        UserPracticeWritingCriterionFeedbackResponseDto dto =
                new UserPracticeWritingCriterionFeedbackResponseDto();
        dto.setId(saved.getId());

        return dto;
    }

    @Override
    @Transactional
    public void delete(String id) {
        UserPracticeWritingCriterionFeedback feedback =
                userPracticeWritingCriterionFeedbackRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException(
                                "User practice writing criterion feedback not found with id: " + id
                        ));

        userPracticeWritingCriterionFeedbackRepository.delete(feedback);
    }

    private void applyIncludes(UserPracticeWritingCriterionFeedback feedback,
                               UserPracticeWritingCriterionFeedbackResponseDto dto,
                               IncludeSpec includes) {

        if (includes.has("authortype")) dto.setAuthorType(feedback.getAuthorType());
        if (includes.has("criterionname")) dto.setCriterionName(feedback.getCriterionName());
        if (includes.has("feedbacktype")) dto.setFeedbackType(feedback.getFeedbackType());
        if (includes.has("label")) dto.setLabel(feedback.getLabel());
        if (includes.has("description")) dto.setDescription(feedback.getDescription());
        if (includes.has("explanation")) dto.setExplanation(feedback.getExplanation());
        if (includes.has("evidencesentences")) dto.setEvidenceSentences(feedback.getEvidenceSentences());
        if (includes.has("recommendedactiondescription")) dto.setRecommendedActionDescription(feedback.getRecommendedActionDescription());
        if (includes.has("recommendedactionexplanation")) dto.setRecommendedActionExplanation(feedback.getRecommendedActionExplanation());
        if (includes.has("createdat")) dto.setCreatedAt(feedback.getCreatedAt());
        if (includes.has("updatedat")) dto.setUpdatedAt(feedback.getUpdatedAt());

        UserPracticeWritingCriterionFeedbackUserResponseDto reviewedByUserDto =
                new UserPracticeWritingCriterionFeedbackUserResponseDto();

        if (feedback.getReviewedByUser()!= null) {
            if (includes.has("reviewedbyuser.userid"))
                reviewedByUserDto.setUserId(feedback.getReviewedByUser().getUserId());
            if (includes.has("reviewedbyuser.email"))
                reviewedByUserDto.setEmail(feedback.getReviewedByUser().getEmail());
            if (includes.has("reviewedbyuser.firstname"))
                reviewedByUserDto.setFirstname(feedback.getReviewedByUser().getFirstname());
            if (includes.has("reviewedbyuser.lastname"))
                reviewedByUserDto.setLastname(feedback.getReviewedByUser().getLastname());
        }

        dto.setReviewedByUser(reviewedByUserDto);
    }
}
