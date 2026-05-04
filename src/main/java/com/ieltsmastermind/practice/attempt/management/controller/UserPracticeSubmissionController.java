package com.ieltsmastermind.practice.attempt.management.controller;

import com.ieltsmastermind.common.query.IncludeParser;
import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.common.response.ApiResponse;
import com.ieltsmastermind.practice.attempt.management.business.interfaces.UserPracticeSubmissionService;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeSubmissionCreateRequestDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeSubmissionResponseDto;
import com.ieltsmastermind.practice.attempt.management.domain.dto.UserPracticeSubmissionUpdateRequestDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/practice-submission")
public class UserPracticeSubmissionController {

    private final UserPracticeSubmissionService userPracticeSubmissionService;

    public UserPracticeSubmissionController(UserPracticeSubmissionService userPracticeSubmissionService) {
        this.userPracticeSubmissionService = userPracticeSubmissionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserPracticeSubmissionResponseDto>> create(
            @Valid @RequestBody UserPracticeSubmissionCreateRequestDto request
    ) {
        try {
            UserPracticeSubmissionResponseDto created = userPracticeSubmissionService.create(request);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Submission created successfully", created));
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
    public ResponseEntity<ApiResponse<List<UserPracticeSubmissionResponseDto>>> getAllByUserId(
            @PathVariable String userId,
            @RequestParam(required = false) String include
    ) {
        try {
            IncludeSpec includes = IncludeParser.parse(include);

            List<UserPracticeSubmissionResponseDto> submissions =
                    userPracticeSubmissionService.getAllByUserId(userId, includes);

            return ResponseEntity.ok(
                    ApiResponse.success("User practice submissions fetched successfully", submissions)
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
    public ResponseEntity<ApiResponse<UserPracticeSubmissionResponseDto>> getById(
            @PathVariable String id,
            @RequestParam(required = false) String include
    ) {
        try {
            IncludeSpec includes = IncludeParser.parse(include);

            UserPracticeSubmissionResponseDto submission =
                    userPracticeSubmissionService.getById(id, includes);

            return ResponseEntity.ok(
                    ApiResponse.success("User practice submission fetched successfully", submission)
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

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserPracticeSubmissionResponseDto>>> getAllUserPracticeSubmissions(
            @RequestParam(required = false) String include
    ) {
        try {
            IncludeSpec includes = IncludeParser.parse(include);

            List<UserPracticeSubmissionResponseDto> submissions =
                    userPracticeSubmissionService.getAll(includes);

            return ResponseEntity.ok(
                    ApiResponse.success("User practice submissions fetched successfully", submissions)
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

    @GetMapping("/tutor-review-requested")
    public ResponseEntity<ApiResponse<List<UserPracticeSubmissionResponseDto>>> getAllTutorReviewRequestedSubmissions(
            @RequestParam(required = false) String include
    ) {
        try {
            IncludeSpec includes = IncludeParser.parse(include);

            List<UserPracticeSubmissionResponseDto> submissions =
                    userPracticeSubmissionService.getAllTutorReviewRequested(includes);

            return ResponseEntity.ok(
                    ApiResponse.success("Tutor review requested submissions fetched successfully", submissions)
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

    @GetMapping("/user/{userId}/latest/{days}")
    public ResponseEntity<ApiResponse<List<UserPracticeSubmissionResponseDto>>> getAllByUserIdAndDays(
            @PathVariable String userId,
            @PathVariable int days,
            @RequestParam(required = false) String include
    ) {
        try {
            IncludeSpec includes = IncludeParser.parse(include);

            List<UserPracticeSubmissionResponseDto> submissions =
                    userPracticeSubmissionService.getAllByUserIdAndDays(userId, days, includes);

            return ResponseEntity.ok(
                    ApiResponse.success("User practice submissions fetched successfully", submissions)
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
    public ResponseEntity<ApiResponse<UserPracticeSubmissionResponseDto>> update(
            @PathVariable String id,
            @Valid @RequestBody UserPracticeSubmissionUpdateRequestDto request
    ) {
        try {
            UserPracticeSubmissionResponseDto updated = userPracticeSubmissionService.update(id, request);
            return ResponseEntity.ok(
                    ApiResponse.success("User practice submission updated successfully", updated)
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
            userPracticeSubmissionService.delete(id);
            return ResponseEntity.ok(
                    ApiResponse.success("User practice submission deleted successfully", null)
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