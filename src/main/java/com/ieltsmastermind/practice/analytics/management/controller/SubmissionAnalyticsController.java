// package: com.ieltsmastermind.practice.analytics.management.controller

package com.ieltsmastermind.practice.analytics.management.controller;

import com.ieltsmastermind.common.response.ApiResponse;
import com.ieltsmastermind.practice.analytics.management.business.interfaces.SubmissionAnalyticsService;
import com.ieltsmastermind.practice.analytics.management.domain.dto.SubmissionAnalyticsSkillCountResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/submission-analytics")
@RequiredArgsConstructor
public class SubmissionAnalyticsController {

    private final SubmissionAnalyticsService submissionAnalyticsService;

    @GetMapping("/user/{userId}/analytics-submission-counts")
    public ResponseEntity<ApiResponse<SubmissionAnalyticsSkillCountResponseDto>> getAnalyticsSubmissionCountsByUserId(
            @PathVariable String userId
    ) {
        try {
            SubmissionAnalyticsSkillCountResponseDto counts =
                    submissionAnalyticsService.getAnalyticsSubmissionCountsByUserId(userId);

            return ResponseEntity.ok(
                    ApiResponse.success(
                            "Submission counts fetched successfully",
                            counts
                    )
            );
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