package com.ieltsmastermind.practice.attempt.management.business.service;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.attempt.management.business.interfaces.TutorUserPracticeSubmissionService;
import com.ieltsmastermind.practice.attempt.management.business.interfaces.UserPracticeSubmissionService;
import com.ieltsmastermind.practice.attempt.management.domain.dto.TutorUserPracticeSubmissionResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.TutorUserPracticeSubmissionSetStatusRequestDto;
import com.ieltsmastermind.practice.attempt.management.domain.entity.TutorUserPracticeSubmission;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeSubmission;
import com.ieltsmastermind.practice.attempt.management.domain.enums.TutorStatus;
import com.ieltsmastermind.practice.attempt.management.persistence.TutorUserPracticeSubmissionRepository;
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
public class TutorUserPracticeSubmissionServiceImpl implements TutorUserPracticeSubmissionService {

    private final TutorUserPracticeSubmissionRepository tutorUserPracticeSubmissionRepository;
    private final UserRepository userRepository;
    private final UserPracticeSubmissionRepository userPracticeSubmissionRepository;

    private final UserPracticeSubmissionService userPracticeSubmissionService;

    private static final List<TutorStatus> ACTIVE_TUTOR_STATUSES = List.of(
            TutorStatus.IN_REVIEW,
            TutorStatus.COMPLETED
    );

    @Override
    @Transactional
    public TutorUserPracticeSubmissionResponseDto setTutorStatus(
            String tutorId,
            String userPracticeSubmissionId,
            TutorUserPracticeSubmissionSetStatusRequestDto request
    ) {

        User tutor = userRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor not found with id: " + tutorId));

        UserPracticeSubmission submission = userPracticeSubmissionRepository.findById(userPracticeSubmissionId)
                .orElseThrow(() -> new RuntimeException(
                        "User practice submission not found with id: " + userPracticeSubmissionId
                ));

        TutorStatus requestedStatus = request.getTutorStatus();

        validateSubmissionCanBeReviewedByTutor(
                tutorId,
                userPracticeSubmissionId,
                requestedStatus
        );

        TutorUserPracticeSubmission tutorUserPracticeSubmission =
                tutorUserPracticeSubmissionRepository
                        .findByTutor_UserIdAndUserPracticeSubmission_Id(tutorId, userPracticeSubmissionId)
                        .orElseGet(() -> {
                            TutorUserPracticeSubmission entity = new TutorUserPracticeSubmission();
                            entity.setTutor(tutor);
                            entity.setUserPracticeSubmission(submission);
                            return entity;
                        });

        tutorUserPracticeSubmission.setTutorStatus(requestedStatus);

        TutorUserPracticeSubmission saved =
                tutorUserPracticeSubmissionRepository.save(tutorUserPracticeSubmission);

        userPracticeSubmissionService.syncTutorStatusFromTutorSubmissions(userPracticeSubmissionId);

        TutorUserPracticeSubmissionResponseDto dto = new TutorUserPracticeSubmissionResponseDto();
        dto.setId(saved.getId());

        return dto;
    }

    @Override
    @Transactional
    public List<TutorUserPracticeSubmissionResponseDto> getAllByTutorId(String tutorId, IncludeSpec includes) {
        List<TutorUserPracticeSubmission> rows =
                tutorUserPracticeSubmissionRepository.findAllByTutor_UserId(tutorId);

        List<TutorUserPracticeSubmissionResponseDto> result = new ArrayList<>();

        for (TutorUserPracticeSubmission row : rows) {
            TutorUserPracticeSubmissionResponseDto dto = new TutorUserPracticeSubmissionResponseDto();
            dto.setId(row.getId());
            applyIncludes(row, dto, includes);
            result.add(dto);
        }

        return result;
    }

    @Override
    @Transactional
    public TutorUserPracticeSubmissionResponseDto getByTutorIdAndUserPracticeSubmissionId(
            String tutorId,
            String userPracticeSubmissionId,
            IncludeSpec includes
    ) {
        TutorUserPracticeSubmission row =
                tutorUserPracticeSubmissionRepository
                        .findByTutor_UserIdAndUserPracticeSubmission_Id(tutorId, userPracticeSubmissionId)
                        .orElse(null);

        TutorUserPracticeSubmissionResponseDto dto = new TutorUserPracticeSubmissionResponseDto();

        if (row == null) {
            dto.setTutorId(tutorId);
            dto.setUserPracticeSubmissionId(userPracticeSubmissionId);
            return dto;
        }

        dto.setId(row.getId());
        applyIncludes(row, dto, includes);

        return dto;
    }

    private void applyIncludes(
            TutorUserPracticeSubmission row,
            TutorUserPracticeSubmissionResponseDto dto,
            IncludeSpec includes
    ) {
        if (includes.has("tutorid")) dto.setTutorId(row.getTutor().getUserId());
        if (includes.has("userpracticesubmissionid")) dto.setUserPracticeSubmissionId(row.getUserPracticeSubmission().getId());
        if (includes.has("tutorstatus")) dto.setTutorStatus(row.getTutorStatus());

    }

    private void validateSubmissionCanBeReviewedByTutor(
            String tutorId,
            String userPracticeSubmissionId,
            TutorStatus requestedStatus
    ) {
        if (!ACTIVE_TUTOR_STATUSES.contains(requestedStatus)) {
            return;
        }

        boolean alreadyReviewedByAnotherTutor =
                tutorUserPracticeSubmissionRepository
                        .existsByUserPracticeSubmission_IdAndTutor_UserIdNotAndTutorStatusIn(
                                userPracticeSubmissionId,
                                tutorId,
                                ACTIVE_TUTOR_STATUSES
                        );

        if (alreadyReviewedByAnotherTutor) {
            throw new RuntimeException(
                    "This submission is already being reviewed or has been completed by another tutor."
            );
        }
    }
}
