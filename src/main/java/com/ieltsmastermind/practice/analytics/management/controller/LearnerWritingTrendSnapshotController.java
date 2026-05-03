package com.ieltsmastermind.practice.analytics.management.controller;

import com.ieltsmastermind.common.response.ApiResponse;
import com.ieltsmastermind.practice.analytics.management.business.interfaces.LearnerWritingTrendSnapshotService;
import com.ieltsmastermind.practice.analytics.management.domain.dto.LearnerWritingTrendSnapshotCreateRequestDto;
import com.ieltsmastermind.practice.analytics.management.domain.dto.LearnerWritingTrendSnapshotResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/learner-writing-trend-snapshot")
@RequiredArgsConstructor
public class LearnerWritingTrendSnapshotController {

    private final LearnerWritingTrendSnapshotService learnerWritingTrendSnapshotService;

    @PostMapping
    public ResponseEntity<ApiResponse<LearnerWritingTrendSnapshotResponseDto>> create(
            @Valid @RequestBody LearnerWritingTrendSnapshotCreateRequestDto request
    ) {
        try {
            LearnerWritingTrendSnapshotResponseDto created =
                    learnerWritingTrendSnapshotService.create(request);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            "Learner writing trend snapshot created successfully",
                            created
                    ));
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
