package com.ieltsmastermind.practice.analytics.management.business.interfaces;

import com.ieltsmastermind.practice.analytics.management.domain.dto.LearnerTrendSnapshotCreateRequestDto;
import com.ieltsmastermind.practice.analytics.management.domain.dto.LearnerTrendSnapshotResponseDto;
import com.ieltsmastermind.practice.analytics.management.domain.entity.LearnerTrendSnapshot;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;

public interface LearnerTrendSnapshotService {
    LearnerTrendSnapshotResponseDto create(LearnerTrendSnapshotCreateRequestDto request);
    LearnerTrendSnapshot createSnapshot(String learnerId, PracticeContentSkill skill);
}
