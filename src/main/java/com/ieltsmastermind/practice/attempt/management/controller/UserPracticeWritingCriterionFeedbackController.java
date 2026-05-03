package com.ieltsmastermind.practice.attempt.management.controller;

import com.ieltsmastermind.common.query.IncludeParser;
import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.common.response.ApiResponse;
import com.ieltsmastermind.practice.attempt.management.business.interfaces.UserPracticeWritingCriterionFeedbackService;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeWritingCriterionFeedbackCreateRequestDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeWritingCriterionFeedbackResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeWritingCriterionFeedbackUpdateRequestDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/practice-writing-criterion-feedback")
public class UserPracticeWritingCriterionFeedbackController {

    private final UserPracticeWritingCriterionFeedbackService userPracticeWritingCriterionFeedbackService;

    public UserPracticeWritingCriterionFeedbackController(
            UserPracticeWritingCriterionFeedbackService userPracticeWritingCriterionFeedbackService
    ) {
        this.userPracticeWritingCriterionFeedbackService = userPracticeWritingCriterionFeedbackService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserPracticeWritingCriterionFeedbackResponseDto>> create(
            @Valid @RequestBody UserPracticeWritingCriterionFeedbackCreateRequestDto request
    ) {
        try {
            UserPracticeWritingCriterionFeedbackResponseDto created =
                    userPracticeWritingCriterionFeedbackService.create(request);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Writing criterion feedback created successfully", created));
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
    public ResponseEntity<ApiResponse<List<UserPracticeWritingCriterionFeedbackResponseDto>>> getAllBySubmissionId(
            @PathVariable String submissionId,
            @RequestParam(required = false) String include
    ) {
        try {
            IncludeSpec includes = IncludeParser.parse(include);

            List<UserPracticeWritingCriterionFeedbackResponseDto> feedbacks =
                    userPracticeWritingCriterionFeedbackService.getAllBySubmissionId(submissionId, includes);

            return ResponseEntity.ok(
                    ApiResponse.success("Writing criterion feedbacks fetched successfully", feedbacks)
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

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserPracticeWritingCriterionFeedbackResponseDto>> update(
            @PathVariable String id,
            @Valid @RequestBody UserPracticeWritingCriterionFeedbackUpdateRequestDto request
    ) {
        try {
            UserPracticeWritingCriterionFeedbackResponseDto updated =
                    userPracticeWritingCriterionFeedbackService.update(id, request);

            return ResponseEntity.ok(
                    ApiResponse.success("Writing criterion feedback updated successfully", updated)
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

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(
            @PathVariable String id
    ) {
        try {
            userPracticeWritingCriterionFeedbackService.delete(id);
            return ResponseEntity.ok(
                    ApiResponse.success("Writing criterion feedback deleted successfully", null)
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
