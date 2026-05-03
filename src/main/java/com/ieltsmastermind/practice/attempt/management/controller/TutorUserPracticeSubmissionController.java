package com.ieltsmastermind.practice.attempt.management.controller;

import com.ieltsmastermind.common.query.IncludeParser;
import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.common.response.ApiResponse;
import com.ieltsmastermind.practice.attempt.management.business.interfaces.TutorUserPracticeSubmissionService;
import com.ieltsmastermind.practice.attempt.management.domain.dto.TutorUserPracticeSubmissionResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.TutorUserPracticeSubmissionSetStatusRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tutor-user-practice-submissions")
@RequiredArgsConstructor
public class TutorUserPracticeSubmissionController {

    private final TutorUserPracticeSubmissionService tutorUserPracticeSubmissionService;

    @PutMapping("/tutor/{tutorId}/submission/{userPracticeSubmissionId}/status")
    public ResponseEntity<ApiResponse<TutorUserPracticeSubmissionResponseDto>> setTutorStatus(
            @PathVariable String tutorId,
            @PathVariable String userPracticeSubmissionId,
            @Valid @RequestBody TutorUserPracticeSubmissionSetStatusRequestDto request
    ) {
        try {
            TutorUserPracticeSubmissionResponseDto updated =
                    tutorUserPracticeSubmissionService.setTutorStatus(
                            tutorId,
                            userPracticeSubmissionId,
                            request
                    );

            return ResponseEntity.ok(
                    ApiResponse.success("Tutor status updated successfully", updated)
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

    @GetMapping("/tutor/{tutorId}")
    public ResponseEntity<ApiResponse<List<TutorUserPracticeSubmissionResponseDto>>> getAllByTutorId(
            @PathVariable String tutorId,
            @RequestParam(required = false) String include
    ) {
        try {
            IncludeSpec includes = IncludeParser.parse(include);

            List<TutorUserPracticeSubmissionResponseDto> items =
                    tutorUserPracticeSubmissionService.getAllByTutorId(tutorId, includes);

            return ResponseEntity.ok(
                    ApiResponse.success("Tutor user practice submissions fetched successfully", items)
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

    @GetMapping("/tutor/{tutorId}/submission/{userPracticeSubmissionId}")
    public ResponseEntity<ApiResponse<TutorUserPracticeSubmissionResponseDto>> getByTutorIdAndUserPracticeSubmissionId(
            @PathVariable String tutorId,
            @PathVariable String userPracticeSubmissionId,
            @RequestParam(required = false) String include
    ) {
        try {
            IncludeSpec includes = IncludeParser.parse(include);

            TutorUserPracticeSubmissionResponseDto item =
                    tutorUserPracticeSubmissionService.getByTutorIdAndUserPracticeSubmissionId(
                            tutorId,
                            userPracticeSubmissionId,
                            includes
                    );

            return ResponseEntity.ok(
                    ApiResponse.success("Tutor user practice submission fetched successfully", item)
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
