package com.ieltsmastermind.practice.attempt.management.business.service;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.practice.attempt.management.business.interfaces.UserPracticeContentProgressService;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeContentProgressResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeContentProgressUpsertRequestDto;
import com.ieltsmastermind.practice.attempt.management.domain.entity.UserPracticeContentProgress;
import com.ieltsmastermind.practice.attempt.management.persistence.UserPracticeContentProgressRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserPracticeContentProgressServiceImpl implements UserPracticeContentProgressService {

    private final UserPracticeContentProgressRepository userPracticeContentProgressRepository;

    @Override
    @Transactional
    public UserPracticeContentProgressResponseDto upsert(
            String userId,
            String practiceContentId,
            UserPracticeContentProgressUpsertRequestDto request
    ) {

        UserPracticeContentProgress progress =
                userPracticeContentProgressRepository
                        .findByUserIdAndPracticeContentId(userId, practiceContentId)
                        .orElseGet(() -> {
                            UserPracticeContentProgress p = new UserPracticeContentProgress();
                            p.setUserId(userId);
                            p.setPracticeContentId(practiceContentId);
                            return p;
                        });

        if (request.getIsBookmarked() != null) {
            progress.setIsBookmarked(request.getIsBookmarked());
        }

        UserPracticeContentProgress saved  = userPracticeContentProgressRepository.save(progress);

        UserPracticeContentProgressResponseDto dto = new UserPracticeContentProgressResponseDto();
        dto.setId(saved.getId());

        return dto;
    }

    @Override
    @Transactional
    public UserPracticeContentProgressResponseDto incrementAttemptCount(
            String userId,
            String practiceContentId
    ) {
        int updatedRows = userPracticeContentProgressRepository
                .incrementAttemptCount(userId, practiceContentId);

        UserPracticeContentProgress progress;

        if (updatedRows == 0) {
            UserPracticeContentProgress newProgress = new UserPracticeContentProgress();
            newProgress.setUserId(userId);
            newProgress.setPracticeContentId(practiceContentId);
            newProgress.setAttemptCount(1);

            progress = userPracticeContentProgressRepository.save(newProgress);
        } else {
            progress = userPracticeContentProgressRepository
                    .findByUserIdAndPracticeContentId(userId, practiceContentId)
                    .orElseThrow(() -> new RuntimeException("User practice content progress not found"));
        }

        UserPracticeContentProgressResponseDto dto = new UserPracticeContentProgressResponseDto();
        dto.setId(progress.getId());

        return dto;
    }

    @Override
    public List<UserPracticeContentProgressResponseDto> getAllByUserId(String userId, IncludeSpec includes) {
        List<UserPracticeContentProgress> rows = userPracticeContentProgressRepository.findAllByUserId(userId);
        List<UserPracticeContentProgressResponseDto> result = new ArrayList<>();

        for (UserPracticeContentProgress row : rows) {
            UserPracticeContentProgressResponseDto dto = new UserPracticeContentProgressResponseDto();
            dto.setId(row.getId());
            applyIncludes(row, dto, includes);
            result.add(dto);
        }

        return result;
    }

    private void applyIncludes(
            UserPracticeContentProgress row,
            UserPracticeContentProgressResponseDto dto,
            IncludeSpec includes
    ) {
        if (includes.has("userid")) dto.setUserId(row.getUserId());
        if (includes.has("practicecontentid")) dto.setPracticeContentId(row.getPracticeContentId());
        if (includes.has("isbookmarked")) dto.setIsBookmarked(row.getIsBookmarked());
        if (includes.has("attemptcount")) dto.setAttemptCount(row.getAttemptCount());
    }
}
