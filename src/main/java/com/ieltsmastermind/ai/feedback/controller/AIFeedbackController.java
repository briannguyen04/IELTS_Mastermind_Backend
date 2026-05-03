package com.ieltsmastermind.ai.feedback.controller;

import com.ieltsmastermind.ai.feedback.business.interfaces.ListeningFeedbackService;
import com.ieltsmastermind.ai.feedback.business.interfaces.ReadingFeedbackService;
import com.ieltsmastermind.ai.feedback.business.interfaces.WritingFeedbackService;
import com.ieltsmastermind.ai.feedback.business.service.LearnerPracticeAIService.WritingFeedbackAsyncService;
import com.ieltsmastermind.ai.feedback.domain.dto.CreateAIFeedbackRequestDto;
import com.ieltsmastermind.ai.feedback.domain.dto.ListeningFeedbackResponseDto;
import com.ieltsmastermind.ai.feedback.domain.dto.ReadingFeedbackResponseDto;
import com.ieltsmastermind.ai.feedback.domain.dto.WritingFeedbackResponseDto;
import com.ieltsmastermind.common.response.ApiResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AIFeedbackController {

    private final ListeningFeedbackService listeningFeedbackService;
    private final ReadingFeedbackService readingFeedbackService;
    private final WritingFeedbackService writingFeedbackService;

    private final WritingFeedbackAsyncService asyncService;


    public AIFeedbackController(ListeningFeedbackService listeningFeedbackService, ReadingFeedbackService readingFeedbackService, WritingFeedbackService writingFeedbackService, WritingFeedbackAsyncService asyncService) {
        this.listeningFeedbackService = listeningFeedbackService;
        this.readingFeedbackService = readingFeedbackService;
        this.writingFeedbackService = writingFeedbackService;
        this.asyncService = asyncService;
    }

    // ==================== LISTENING =======================

    @PostMapping("/listening-submissions/ai-feedback")
    public ResponseEntity<ApiResponse<String>> createListeningFeedback(
            @RequestBody CreateAIFeedbackRequestDto request
    ) {
        try {
            if (request.getSubmissionId() == null || request.getSubmissionId().isBlank()) {
                throw new RuntimeException("submissionId is required");
            }
            String submissionId = request.getSubmissionId();

            listeningFeedbackService.createListeningFeedback(submissionId);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            "Listening AI feedback created successfully",
                            submissionId
                    ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail(e.getMessage(), null));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error"));
        }
    }

    @GetMapping("/listening-submissions/{submissionId}/ai-feedback")
    public ResponseEntity<ApiResponse<ListeningFeedbackResponseDto>> getListeningFeedback(
            @PathVariable String submissionId
    ) {
        try {
            ListeningFeedbackResponseDto feedback =
                    listeningFeedbackService.getListeningFeedback(submissionId);

            return ResponseEntity.ok(
                    ApiResponse.success(
                            "Listening AI feedback fetched successfully",
                            feedback
                    )
            );

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail(e.getMessage(), null));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error"));
        }
    }

     //==================== READING =========================

    @PostMapping("/reading-submissions/ai-feedback")
    public ResponseEntity<ApiResponse<String>> createReadingFeedback(
            @RequestBody CreateAIFeedbackRequestDto request
    ) {
        try {
            if (request.getSubmissionId() == null || request.getSubmissionId().isBlank()) {
                throw new RuntimeException("submissionId is required");
            }
            String submissionId = request.getSubmissionId();

            readingFeedbackService.createReadingFeedback(submissionId);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            "Reading AI feedback created successfully",
                            submissionId
                    ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail(e.getMessage(), null));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error"));
        }
    }

    @GetMapping("/reading-submissions/{submissionId}/ai-feedback")
    public ResponseEntity<ApiResponse<ReadingFeedbackResponseDto>> getReadingFeedback(
            @PathVariable String submissionId
    ) {
        try {

            ReadingFeedbackResponseDto feedback =
                    readingFeedbackService.getReadingFeedback(submissionId);

            return ResponseEntity.ok(
                    ApiResponse.success(
                            "Reading AI feedback fetched successfully",
                            feedback
                    )
            );

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail(e.getMessage(), null));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error"));
        }
    }

    // ==================== WRITING =========================
    @PostMapping("/writing-submissions/ai-feedback")
    public ResponseEntity<ApiResponse<String>> createWritingFeedback(
            @RequestBody CreateAIFeedbackRequestDto request
    ) {

        try {
            if (request.getSubmissionId() == null || request.getSubmissionId().isBlank()) {
                throw new RuntimeException("submissionId is required");
            }
            String submissionId = request.getSubmissionId();

            asyncService.generateFeedback(submissionId);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            "Writing AI feedback is being generated",
                            submissionId
                    ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail(e.getMessage(), null));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error"));
        }
    }

    @GetMapping("/writing-submissions/{submissionId}/ai-feedback")
    public ResponseEntity<ApiResponse<WritingFeedbackResponseDto>> getWritingFeedback(
            @PathVariable String submissionId
    ) {
        try {
            WritingFeedbackResponseDto feedback =
                    writingFeedbackService.getWritingFeedback(submissionId);

            return ResponseEntity.ok(
                    ApiResponse.success(
                            "Writing AI feedback fetched successfully",
                            feedback
                    )
            );

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail(e.getMessage(), null));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error"));
        }
    }
}