package com.ieltsmastermind.practice.attempt.management.controller;

import com.ieltsmastermind.common.query.IncludeParser;
import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.common.response.ApiResponse;
import com.ieltsmastermind.practice.attempt.management.business.interfaces.UserPracticeSubmissionAnswerService;
import com.ieltsmastermind.practice.attempt.management.domain.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/practice-submission-answer")
public class UserPracticeSubmissionAnswerController {

    private final UserPracticeSubmissionAnswerService userPracticeSubmissionAnswerService;

    public UserPracticeSubmissionAnswerController(UserPracticeSubmissionAnswerService userPracticeSubmissionAnswerService) {
        this.userPracticeSubmissionAnswerService = userPracticeSubmissionAnswerService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserPracticeSubmissionAnswerResponseDto>> create(
            @Valid @RequestBody UserPracticeSubmissionAnswerCreateRequestDto request
    ) {
        try {
            UserPracticeSubmissionAnswerResponseDto created = userPracticeSubmissionAnswerService.create(request);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Submission answer created successfully", created));
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

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<UserPracticeSubmissionAnswerResponseDto>>> createBulk(
            @Valid @RequestBody UserPracticeSubmissionAnswerBulkCreateRequestDto request
    ) {
        try {
            List<UserPracticeSubmissionAnswerResponseDto> created =
                    userPracticeSubmissionAnswerService.createBulk(request);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Submission answers created successfully", created));
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

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<ApiResponse<List<UserPracticeSubmissionAnswerResponseDto>>> getAllBySubmissionId(
            @PathVariable String submissionId,
            @RequestParam(required = false) String include
    ) {
        try {
            IncludeSpec includes = IncludeParser.parse(include);

            List<UserPracticeSubmissionAnswerResponseDto> answers =
                    userPracticeSubmissionAnswerService.getAllBySubmissionId(submissionId, includes);

            return ResponseEntity.ok(
                    ApiResponse.success("Submission answers fetched successfully", answers)
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

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserPracticeSubmissionAnswerResponseDto>> getById(
            @PathVariable String id,
            @RequestParam(required = false) String include
    ) {
        try {
            IncludeSpec includes = IncludeParser.parse(include);

            UserPracticeSubmissionAnswerResponseDto dto =
                    userPracticeSubmissionAnswerService.getById(id, includes);

            return ResponseEntity.ok(
                    ApiResponse.success("Submission answer fetched successfully", dto)
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
