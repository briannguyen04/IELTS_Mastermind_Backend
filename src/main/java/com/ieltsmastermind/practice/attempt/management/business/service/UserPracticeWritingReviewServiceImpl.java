package com.ieltsmastermind.practice.attempt.management.business.service;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.attempt.management.business.interfaces.UserPracticeWritingReviewService;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeWritingReviewResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeWritingReviewUpdateRequestDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeWritingReviewUserResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeWritingAnswer;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeWritingReview;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeWritingAnswerRepository;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeWritingReviewRepository;
import com.ieltsmastermind.user.management.domain.entity.User;
import com.ieltsmastermind.user.management.persistence.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserPracticeWritingReviewServiceImpl implements UserPracticeWritingReviewService {

    private final UserPracticeWritingReviewRepository userPracticeWritingReviewRepository;
    private final UserPracticeWritingAnswerRepository userPracticeWritingAnswerRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public List<UserPracticeWritingReviewResponseDto> getAllByWritingAnswerId(
            String writingAnswerId,
            IncludeSpec includes
    ) {
        List<UserPracticeWritingReview> reviews =
                userPracticeWritingReviewRepository
                        .findAllByWritingAnswer_IdOrderByCreatedAtAsc(writingAnswerId);

        List<UserPracticeWritingReviewResponseDto> result = new ArrayList<>();

        for (UserPracticeWritingReview review : reviews) {
            UserPracticeWritingReviewResponseDto dto = new UserPracticeWritingReviewResponseDto();
            dto.setId(review.getId());

            applyIncludes(review, dto, includes);

            result.add(dto);
        }

        return result;
    }

    @Override
    @Transactional
    public UserPracticeWritingReviewResponseDto upsert(
            String reviewedByUserId,
            String writingAnswerId,
            UserPracticeWritingReviewUpdateRequestDto request
    ) {
        UserPracticeWritingReview review = userPracticeWritingReviewRepository
                .findByReviewedByUser_UserIdAndWritingAnswer_Id(
                        reviewedByUserId,
                        writingAnswerId
                )
                .orElseGet(() -> {
                    UserPracticeWritingAnswer writingAnswer = userPracticeWritingAnswerRepository
                            .findById(writingAnswerId)
                            .orElseThrow(() -> new RuntimeException(
                                    "User practice writing answer not found with id: " + writingAnswerId
                            ));

                    User reviewedByUser = userRepository
                            .findById(reviewedByUserId)
                            .orElseThrow(() -> new RuntimeException(
                                    "User not found with id: " + reviewedByUserId
                            ));

                    UserPracticeWritingReview newReview = new UserPracticeWritingReview();
                    newReview.setWritingAnswer(writingAnswer);
                    newReview.setReviewedByUser(reviewedByUser);
                    return newReview;
                });

        if (request.getTutorTaskAchievementBand() != null) review.setTutorTaskAchievementBand(request.getTutorTaskAchievementBand());
        if (request.getTutorTaskResponseBand() != null) review.setTutorTaskResponseBand(request.getTutorTaskResponseBand());
        if (request.getTutorCoherenceAndCohesionBand() != null) review.setTutorCoherenceAndCohesionBand(request.getTutorCoherenceAndCohesionBand());
        if (request.getTutorLexicalResourceBand() != null) review.setTutorLexicalResourceBand(request.getTutorLexicalResourceBand());
        if (request.getTutorGrammaticalRangeAndAccuracyBand() != null) review.setTutorGrammaticalRangeAndAccuracyBand(request.getTutorGrammaticalRangeAndAccuracyBand());

        double overallTutorBand =
                (
                        review.getTutorTaskAchievementBand()
                                + review.getTutorTaskResponseBand()
                                + review.getTutorCoherenceAndCohesionBand()
                                + review.getTutorLexicalResourceBand()
                                + review.getTutorGrammaticalRangeAndAccuracyBand()
                ) / 4.0;

        review.setOverallTutorBand(overallTutorBand);

        UserPracticeWritingReview saved =
                userPracticeWritingReviewRepository.save(review);

        UserPracticeWritingReviewResponseDto dto =
                new UserPracticeWritingReviewResponseDto();
        dto.setId(saved.getId());

        return dto;
    }

    private void applyIncludes(
            UserPracticeWritingReview review,
            UserPracticeWritingReviewResponseDto dto,
            IncludeSpec includes
    ) {
        if (includes.has("overalltutorband")) dto.setOverallTutorBand(review.getOverallTutorBand());
        if (includes.has("tutortaskresponseband")) dto.setTutorTaskResponseBand(review.getTutorTaskResponseBand());
        if (includes.has("tutortaskachievementband")) dto.setTutorTaskAchievementBand(review.getTutorTaskAchievementBand());
        if (includes.has("tutorcoherenceandcohesionband")) dto.setTutorCoherenceAndCohesionBand(review.getTutorCoherenceAndCohesionBand());
        if (includes.has("tutorlexicalresourceband")) dto.setTutorLexicalResourceBand(review.getTutorLexicalResourceBand());
        if (includes.has("tutorgrammaticalrangeandaccuracyband")) dto.setTutorGrammaticalRangeAndAccuracyBand(review.getTutorGrammaticalRangeAndAccuracyBand());

        UserPracticeWritingReviewUserResponseDto reviewedByUserDto =
                new UserPracticeWritingReviewUserResponseDto();

        if (includes.has("reviewedbyuser.userid")) reviewedByUserDto.setUserId(review.getReviewedByUser().getUserId());
        if (includes.has("reviewedbyuser.email")) reviewedByUserDto.setEmail(review.getReviewedByUser().getEmail());
        if (includes.has("reviewedbyuser.firstname")) reviewedByUserDto.setFirstname(review.getReviewedByUser().getFirstname());
        if (includes.has("reviewedbyuser.lastname")) reviewedByUserDto.setLastname(review.getReviewedByUser().getLastname());


        dto.setReviewedByUser(reviewedByUserDto);
    }
}
