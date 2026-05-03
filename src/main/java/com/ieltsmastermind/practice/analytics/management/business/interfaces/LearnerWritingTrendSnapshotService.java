package com.ieltsmastermind.practice.analytics.management.business.interfaces;

import com.ieltsmastermind.practice.analytics.management.domain.dto.LearnerWritingTrendSnapshotCreateRequestDto;
import com.ieltsmastermind.practice.analytics.management.domain.dto.LearnerWritingTrendSnapshotResponseDto;
import com.ieltsmastermind.practice.analytics.management.domain.entity.LearnerWritingTrendSnapshot;

public interface LearnerWritingTrendSnapshotService {

    LearnerWritingTrendSnapshotResponseDto create(
            LearnerWritingTrendSnapshotCreateRequestDto request
    );

    LearnerWritingTrendSnapshot createSnapshot(String learnerId);
}
