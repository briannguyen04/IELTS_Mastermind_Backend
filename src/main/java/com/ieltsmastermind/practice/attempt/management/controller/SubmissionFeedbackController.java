package com.ieltsmastermind.practice.attempt.management.controller;

import com.ieltsmastermind.common.query.IncludeParser;
import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.common.response.ApiResponse;
import com.ieltsmastermind.practice.attempt.management.business.interfaces.SubmissionFeedbackService;
import com.ieltsmastermind.practice.attempt.management.domain.dto.SubmissionFeedbackCreateRequestDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.SubmissionFeedbackResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.SubmissionFeedbackUpdateRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/submission-feedback")
@RequiredArgsConstructor
public class SubmissionFeedbackController {
    private final SubmissionFeedbackService submissionFeedbackService;

    @PostMapping
    public ResponseEntity<ApiResponse<SubmissionFeedbackResponseDto>> create(
            @Valid @RequestBody SubmissionFeedbackCreateRequestDto request
    ) {
        try {
            SubmissionFeedbackResponseDto created = submissionFeedbackService.create(request);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Submission feedback created successfully", created));
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
    public ResponseEntity<ApiResponse<List<SubmissionFeedbackResponseDto>>> getAllBySubmissionId(
            @PathVariable String submissionId,
            @RequestParam(required = false) String include
    ) {
        try {
            IncludeSpec includes = IncludeParser.parse(include);

            List<SubmissionFeedbackResponseDto> feedbackList =
                    submissionFeedbackService.getAllBySubmissionId(submissionId, includes);

            return ResponseEntity.ok(
                    ApiResponse.success("Submission feedback fetched successfully", feedbackList)
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
    public ResponseEntity<ApiResponse<SubmissionFeedbackResponseDto>> update(
            @PathVariable String id,
            @Valid @RequestBody SubmissionFeedbackUpdateRequestDto request
    ) {
        try {
            SubmissionFeedbackResponseDto updated = submissionFeedbackService.update(id, request);
            return ResponseEntity.ok(
                    ApiResponse.success("Submission feedback updated successfully", updated)
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
            submissionFeedbackService.delete(id);
            return ResponseEntity.ok(
                    ApiResponse.success("Submission feedback deleted successfully", null)
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
