package com.ieltsmastermind.practice.analytics.management.controller;

import com.ieltsmastermind.common.response.ApiResponse;
import com.ieltsmastermind.practice.analytics.management.business.interfaces.LearnerAnalyticsSnapshotService;
import com.ieltsmastermind.practice.analytics.management.domain.dto.LearnerAnalyticsSnapshotCreateRequestDto;
import com.ieltsmastermind.practice.analytics.management.domain.dto.LearnerAnalyticsSnapshotResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learner-analytics-snapshot")
@RequiredArgsConstructor
public class LearnerAnalyticsSnapshotController {

    private final LearnerAnalyticsSnapshotService learnerAnalyticsSnapshotService;

    @PostMapping
    public ResponseEntity<ApiResponse<LearnerAnalyticsSnapshotResponseDto>> create(
            @Valid @RequestBody LearnerAnalyticsSnapshotCreateRequestDto request
    ) {
        try {
            LearnerAnalyticsSnapshotResponseDto created = learnerAnalyticsSnapshotService.create(request);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Learner analytics snapshot created successfully", created));
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
