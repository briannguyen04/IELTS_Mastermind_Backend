package com.ieltsmastermind.practice.attempt.management.controller;

import com.ieltsmastermind.common.query.IncludeParser;
import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.common.response.ApiResponse;
import com.ieltsmastermind.practice.attempt.management.business.interfaces.UserPracticeWritingReviewService;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeWritingReviewResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeWritingReviewUpdateRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/practice-writing-review")
@RequiredArgsConstructor
public class UserPracticeWritingReviewController {

    private final UserPracticeWritingReviewService userPracticeWritingReviewService;

    @GetMapping("/writing-answer/{writingAnswerId}")
    public ResponseEntity<ApiResponse<List<UserPracticeWritingReviewResponseDto>>> getAllByWritingAnswerId(
            @PathVariable String writingAnswerId,
            @RequestParam(required = false) String include
    ) {
        try {
            IncludeSpec includes = IncludeParser.parse(include);

            List<UserPracticeWritingReviewResponseDto> reviews =
                    userPracticeWritingReviewService.getAllByWritingAnswerId(writingAnswerId, includes);

            return ResponseEntity.ok(
                    ApiResponse.success("Writing reviews fetched successfully", reviews)
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

    @PutMapping("/reviewed-by-user/{reviewedByUserId}/writing-answer/{writingAnswerId}")
    public ResponseEntity<ApiResponse<UserPracticeWritingReviewResponseDto>> upsert(
            @PathVariable String reviewedByUserId,
            @PathVariable String writingAnswerId,
            @Valid @RequestBody UserPracticeWritingReviewUpdateRequestDto request
    ) {
        try {
            UserPracticeWritingReviewResponseDto upserted = userPracticeWritingReviewService.upsert(
                    reviewedByUserId,
                    writingAnswerId,
                    request
            );

            return ResponseEntity.ok(
                    ApiResponse.success("Writing review updated successfully", upserted)
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
