package com.ieltsmastermind.practice.analytics.management.controller;

import com.ieltsmastermind.common.response.ApiResponse;
import com.ieltsmastermind.practice.analytics.management.business.interfaces.LearnerWritingAnalyticsSnapshotService;
import com.ieltsmastermind.practice.analytics.management.domain.dto.LearnerWritingAnalyticsSnapshotCreateRequestDto;
import com.ieltsmastermind.practice.analytics.management.domain.dto.LearnerWritingAnalyticsSnapshotResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/learner-writing-analytics-snapshot")
@RequiredArgsConstructor
public class LearnerWritingAnalyticsSnapshotController {

    private final LearnerWritingAnalyticsSnapshotService learnerWritingAnalyticsSnapshotService;

    @PostMapping
    public ResponseEntity<ApiResponse<LearnerWritingAnalyticsSnapshotResponseDto>> create(
            @Valid @RequestBody LearnerWritingAnalyticsSnapshotCreateRequestDto request
    ) {
        try {
            LearnerWritingAnalyticsSnapshotResponseDto created =
                    learnerWritingAnalyticsSnapshotService.create(request);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            "Learner writing analytics snapshot created successfully",
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
