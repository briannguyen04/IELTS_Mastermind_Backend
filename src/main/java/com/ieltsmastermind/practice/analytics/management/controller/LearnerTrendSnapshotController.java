package com.ieltsmastermind.practice.analytics.management.controller;

import com.ieltsmastermind.common.response.ApiResponse;
import com.ieltsmastermind.practice.analytics.management.business.interfaces.LearnerTrendSnapshotService;
import com.ieltsmastermind.practice.analytics.management.domain.dto.LearnerTrendSnapshotCreateRequestDto;
import com.ieltsmastermind.practice.analytics.management.domain.dto.LearnerTrendSnapshotResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learner-trend-snapshot")
@RequiredArgsConstructor
public class LearnerTrendSnapshotController {

    private final LearnerTrendSnapshotService learnerTrendSnapshotService;

    @PostMapping
    public ResponseEntity<ApiResponse<LearnerTrendSnapshotResponseDto>> create(
            @Valid @RequestBody LearnerTrendSnapshotCreateRequestDto request
    ) {
        try {
            LearnerTrendSnapshotResponseDto created = learnerTrendSnapshotService.create(request);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Learner trend snapshot created successfully", created));
        } catch (RuntimeException e) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.fail(e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error"));
        }
    }
}
