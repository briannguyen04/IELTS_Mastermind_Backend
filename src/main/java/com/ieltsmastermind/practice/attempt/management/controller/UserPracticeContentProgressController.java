package com.ieltsmastermind.practice.attempt.management.controller;

import com.ieltsmastermind.common.query.IncludeParser;
import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.common.response.ApiResponse;
import com.ieltsmastermind.practice.attempt.management.business.interfaces.UserPracticeContentProgressService;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeContentProgressResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeContentProgressUpsertRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-practice-content-progress")
@RequiredArgsConstructor
public class UserPracticeContentProgressController {

    private final UserPracticeContentProgressService userPracticeContentProgressService;

    @PutMapping("/user/{userId}/practice-content/{practiceContentId}")
    public ResponseEntity<ApiResponse<UserPracticeContentProgressResponseDto>> upsert(
            @PathVariable String userId,
            @PathVariable String practiceContentId,
            @Valid @RequestBody UserPracticeContentProgressUpsertRequestDto request
    ) {
        try {
            UserPracticeContentProgressResponseDto updated =
                    userPracticeContentProgressService.upsert(userId, practiceContentId, request);

            return ResponseEntity.ok(
                    ApiResponse.success("User practice content progress updated successfully", updated)
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

    @PostMapping("/user/{userId}/practice-content/{practiceContentId}/attempt-count/increment")
    public ResponseEntity<ApiResponse<UserPracticeContentProgressResponseDto>> incrementAttemptCount(
            @PathVariable String userId,
            @PathVariable String practiceContentId
    ) {
        try {
            UserPracticeContentProgressResponseDto updated =
                    userPracticeContentProgressService.incrementAttemptCount(userId, practiceContentId);

            return ResponseEntity.ok(
                    ApiResponse.success("User attempt count incremented successfully", updated)
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

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<UserPracticeContentProgressResponseDto>>> getAllByUserId(
            @PathVariable String userId,
            @RequestParam(required = false) String include
    ) {
        try {
            IncludeSpec includes = IncludeParser.parse(include);

            List<UserPracticeContentProgressResponseDto> items =
                    userPracticeContentProgressService.getAllByUserId(userId, includes);

            return ResponseEntity.ok(
                    ApiResponse.success("User practice content progress fetched successfully", items)
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
