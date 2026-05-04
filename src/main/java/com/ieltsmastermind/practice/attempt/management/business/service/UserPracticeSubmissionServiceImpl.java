package com.ieltsmastermind.practice.attempt.management.business.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieltsmastermind.common.json.JsonConverter;
import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.attempt.management.business.interfaces.UserPracticeSubmissionService;
import com.ieltsmastermind.practice.attempt.management.domain.dto.*;
import com.ieltsmastermind.practice.attempt.management.domain.entity.SubmissionQuestionTypeAccuracy;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeSubmission;
import com.ieltsmastermind.practice.attempt.management.domain.enums.TutorStatus;
import com.ieltsmastermind.practice.attempt.management.persistence.TutorUserPracticeSubmissionRepository;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeSubmissionRepository;
import com.ieltsmastermind.practice.attempt.management.persistence.projection.UserPracticeSubmissionSkillCountProjection;
import com.ieltsmastermind.practice.content.management.domain.entity.PracticeContent;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;
import com.ieltsmastermind.practice.content.management.persistence.PracticeContentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserPracticeSubmissionServiceImpl implements UserPracticeSubmissionService {

    private final UserPracticeSubmissionRepository userPracticeSubmissionRepository;
    private final PracticeContentRepository practiceContentRepository;
    private final TutorUserPracticeSubmissionRepository tutorUserPracticeSubmissionRepository;

    private final JsonConverter jsonConverter;

    @Override
    @Transactional
    public UserPracticeSubmissionResponseDto create(UserPracticeSubmissionCreateRequestDto request) {

        UserPracticeSubmission submission = new UserPracticeSubmission();
        submission.setUserId(request.getUserId());
        submission.setPracticeContentId(request.getPracticeContentId());
        submission.setTimeSpentSeconds(request.getTimeSpentSeconds());
        submission.setSubmittedAt(LocalDateTime.now());

        JsonNode learnerActivitiesNode = jsonConverter.toJsonNode(request.getLearnerTestActivities());
        submission.setLearnerTestActivities(learnerActivitiesNode);

        UserPracticeSubmission saved = userPracticeSubmissionRepository.save(submission);

        incrementPracticeContentAttemptCount(request.getPracticeContentId());

        UserPracticeSubmissionResponseDto responseDto = new UserPracticeSubmissionResponseDto();
        responseDto.setId(saved.getId());

        return responseDto;
    }

    @Override
    public List<UserPracticeSubmissionResponseDto> getAllByUserId(String userId, IncludeSpec includes) {
        List<UserPracticeSubmission> submissions =
                userPracticeSubmissionRepository.findAllByUserIdOrderBySubmittedAtDesc(userId);

        List<UserPracticeSubmissionResponseDto> result = new ArrayList<>();

        for (UserPracticeSubmission submission : submissions) {
            UserPracticeSubmissionResponseDto dto = new UserPracticeSubmissionResponseDto();
            dto.setId(submission.getId());
            applyIncludes(submission, dto, includes);
            result.add(dto);
        }

        return result;
    }

    @Override
    public UserPracticeSubmissionResponseDto getById(String id, IncludeSpec includes) {
        UserPracticeSubmission submission = userPracticeSubmissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        UserPracticeSubmissionResponseDto dto = new UserPracticeSubmissionResponseDto();
        dto.setId(submission.getId());
        applyIncludes(submission, dto, includes);

        return dto;
    }

    @Override
    @Transactional
    public List<UserPracticeSubmissionResponseDto> getAll(IncludeSpec includes) {
        List<UserPracticeSubmission> submissions = userPracticeSubmissionRepository.findAllByOrderBySubmittedAtDesc();
        List<UserPracticeSubmissionResponseDto> result = new ArrayList<>();

        for (UserPracticeSubmission submission : submissions) {
            UserPracticeSubmissionResponseDto dto = new UserPracticeSubmissionResponseDto();
            dto.setId(submission.getId());
            applyIncludes(submission, dto, includes);
            result.add(dto);
        }

        return result;
    }

    @Override
    @Transactional
    public List<UserPracticeSubmissionResponseDto> getAllTutorReviewRequested(IncludeSpec includes) {
        List<UserPracticeSubmission> submissions =
                userPracticeSubmissionRepository.findByIsTutorReviewRequestedTrueOrderBySubmittedAtDesc();

        List<UserPracticeSubmissionResponseDto> result = new ArrayList<>();

        for (UserPracticeSubmission submission : submissions) {
            UserPracticeSubmissionResponseDto dto = new UserPracticeSubmissionResponseDto();
            dto.setId(submission.getId());
            applyIncludes(submission, dto, includes);
            result.add(dto);
        }

        return result;
    }

    @Override
    public List<UserPracticeSubmissionResponseDto> getAllByUserIdAndDays(String userId, int days, IncludeSpec includes) {
        if (days <= 0) {
            throw new RuntimeException("Days must be greater than 0");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fromDate = now.minusDays(days);

        List<UserPracticeSubmission> submissions =
                userPracticeSubmissionRepository
                        .findAllByUserIdAndSubmittedAtBetweenOrderBySubmittedAtDesc(userId, fromDate, now);

        List<UserPracticeSubmissionResponseDto> result = new ArrayList<>();

        for (UserPracticeSubmission submission : submissions) {
            UserPracticeSubmissionResponseDto dto = new UserPracticeSubmissionResponseDto();
            dto.setId(submission.getId());
            applyIncludes(submission, dto, includes);
            result.add(dto);
        }

        return result;
    }

    @Override
    @Transactional
    public UserPracticeSubmissionResponseDto update(String id, UserPracticeSubmissionUpdateRequestDto request) {
        UserPracticeSubmission submission = userPracticeSubmissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User practice submission not found with id: " + id));

        if (request.getIsTutorReviewRequested() != null) submission.setTutorReviewRequested(request.getIsTutorReviewRequested());

        UserPracticeSubmission saved = userPracticeSubmissionRepository.save(submission);

        UserPracticeSubmissionResponseDto dto = new UserPracticeSubmissionResponseDto();
        dto.setId(saved.getId());

        return dto;
    }

    @Override
    @Transactional
    public void syncTutorStatusFromTutorSubmissions(String userPracticeSubmissionId) {
        UserPracticeSubmission submission = userPracticeSubmissionRepository.findByIdForUpdate(userPracticeSubmissionId)
                .orElseThrow(() -> new RuntimeException(
                        "User practice submission not found with id: " + userPracticeSubmissionId
                ));

        TutorStatus newStatus = resolveTutorStatus(userPracticeSubmissionId);

        submission.setTutorStatus(newStatus);
        userPracticeSubmissionRepository.save(submission);
    }

    @Override
    @Transactional
    public void delete(String id) {
        UserPracticeSubmission submission = userPracticeSubmissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User practice submission not found with id: " + id));

        userPracticeSubmissionRepository.delete(submission);
    }

    private void applyIncludes(
            UserPracticeSubmission submission,
            UserPracticeSubmissionResponseDto dto,
            IncludeSpec includes
    ) {
        if (includes.has("userid")) {
            dto.setUserId(submission.getUserId());
        }

        if (includes.has("practicecontentid")) {
            dto.setPracticeContentId(submission.getPracticeContentId());
        }

        if (includes.has("timespentseconds")) {
            dto.setTimeSpentSeconds(submission.getTimeSpentSeconds());
        }

        if (includes.has("submittedat")) {
            dto.setSubmittedAt(submission.getSubmittedAt());
        }

        if (includes.has("score")) {
            dto.setScore(submission.getScore());
        }

        if (includes.has("correctanswerpercentage")) {
            dto.setCorrectAnswerPercentage(submission.getCorrectAnswerPercentage());
        }

        if (includes.has("correctanswercount")) {
            dto.setCorrectAnswerCount(submission.getCorrectAnswerCount());
        }

        if (includes.has("wronganswercount")) {
            dto.setWrongAnswerCount(submission.getWrongAnswerCount());
        }

        if (includes.has("skipanswercount")) {
            dto.setSkipAnswerCount(submission.getSkipAnswerCount());
        }

        if (includes.has("istutorreviewrequested")) {
            dto.setIsTutorReviewRequested(submission.isTutorReviewRequested());
        }

        if (includes.has("tutorstatus")) {
            dto.setTutorStatus(submission.getTutorStatus());
        }

        boolean hasUserInclude =
                includes.has("user.userid")
                        || includes.has("user.email")
                        || includes.has("user.firstname")
                        || includes.has("user.lastname");

        if (hasUserInclude && submission.getUser() != null) {
            UserPracticeSubmissionUserResponseDto userDto =
                    new UserPracticeSubmissionUserResponseDto();

            if (includes.has("user.userid")) {
                userDto.setUserId(submission.getUser().getUserId());
            }

            if (includes.has("user.email")) {
                userDto.setEmail(submission.getUser().getEmail());
            }

            if (includes.has("user.firstname")) {
                userDto.setFirstname(submission.getUser().getFirstname());
            }

            if (includes.has("user.lastname")) {
                userDto.setLastname(submission.getUser().getLastname());
            }

            dto.setUser(userDto);
        }

        boolean hasPracticeContentInclude =
                includes.has("practicecontent.id")
                        || includes.has("practicecontent.skill")
                        || includes.has("practicecontent.task")
                        || includes.has("practicecontent.title");

        if (hasPracticeContentInclude && submission.getPracticeContent() != null) {
            UserPracticeSubmissionPracticeContentResponseDto practiceContentDto =
                    new UserPracticeSubmissionPracticeContentResponseDto();

            if (includes.has("practicecontent.id")) {
                practiceContentDto.setId(submission.getPracticeContent().getId());
            }

            if (includes.has("practicecontent.skill")) {
                practiceContentDto.setSkill(submission.getPracticeContent().getSkill());
            }

            if (includes.has("practicecontent.task")) {
                practiceContentDto.setTask(submission.getPracticeContent().getTask());
            }

            if (includes.has("practicecontent.title")) {
                practiceContentDto.setTitle(submission.getPracticeContent().getTitle());
            }

            dto.setPracticeContent(practiceContentDto);
        }

        boolean hasQuestionTypeAccuraciesInclude =
                includes.has("questiontypeaccuracies.questiontype")
                        || includes.has("questiontypeaccuracies.correctanswerpercentage");

        if (hasQuestionTypeAccuraciesInclude
                && submission.getQuestionTypeAccuracies() != null) {
            List<SubmissionQuestionTypeAccuracyResponseDto> accuracyDtos =
                    new ArrayList<>();

            for (SubmissionQuestionTypeAccuracy accuracy
                    : submission.getQuestionTypeAccuracies()) {
                SubmissionQuestionTypeAccuracyResponseDto accuracyDto =
                        new SubmissionQuestionTypeAccuracyResponseDto();

                if (includes.has("questiontypeaccuracies.questiontype")) {
                    accuracyDto.setQuestionType(accuracy.getQuestionType());
                }

                if (includes.has("questiontypeaccuracies.correctanswerpercentage")) {
                    accuracyDto.setCorrectAnswerPercentage(
                            accuracy.getCorrectAnswerPercentage()
                    );
                }

                accuracyDtos.add(accuracyDto);
            }

            dto.setQuestionTypeAccuracies(accuracyDtos);
        }
    }

    private void incrementPracticeContentAttemptCount(String practiceContentId) {
        int updated = practiceContentRepository.incrementAttemptCount(practiceContentId);
        if (updated == 0) {
            throw new IllegalArgumentException("PracticeContent not found: " + practiceContentId);
        }
    }

    private TutorStatus resolveTutorStatus(String userPracticeSubmissionId) {
        boolean hasCompletedTutor = tutorUserPracticeSubmissionRepository
                .existsByUserPracticeSubmission_IdAndTutorStatus(
                        userPracticeSubmissionId,
                        TutorStatus.COMPLETED
                );

        if (hasCompletedTutor) {
            return TutorStatus.COMPLETED;
        }

        boolean hasInReviewTutor = tutorUserPracticeSubmissionRepository
                .existsByUserPracticeSubmission_IdAndTutorStatus(
                        userPracticeSubmissionId,
                        TutorStatus.IN_REVIEW
                );

        if (hasInReviewTutor) {
            return TutorStatus.IN_REVIEW;
        }

        return TutorStatus.PENDING;
    }
}
