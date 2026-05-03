package com.ieltsmastermind.practice.content.management.controller;

import com.ieltsmastermind.common.query.IncludeParser;
import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.common.response.ApiResponse;
import com.ieltsmastermind.practice.content.management.business.interfaces.PracticeContentService;
import com.ieltsmastermind.practice.content.management.business.interfaces.PracticeQuestionService;
import com.ieltsmastermind.practice.content.management.domain.dto.PracticeQuestionCreateRequestDto;
import com.ieltsmastermind.practice.content.management.domain.dto.PracticeQuestionResponseDto;
import com.ieltsmastermind.practice.content.management.domain.dto.PracticeQuestionUpdateRequestDto;
import com.ieltsmastermind.practice.content.management.domain.entity.PracticeQuestion;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/practice-question")
public class PracticeQuestionManagementController {

    private final PracticeQuestionService practiceQuestionService;

    public PracticeQuestionManagementController(PracticeQuestionService practiceQuestionService) {
        this.practiceQuestionService = practiceQuestionService;
    }

    @PostMapping("/{practiceContentId}")
    public ResponseEntity<ApiResponse<PracticeQuestionResponseDto>> create(
            @PathVariable String practiceContentId,
            @Valid @RequestBody PracticeQuestionCreateRequestDto request
    ) {
        try {
            PracticeQuestionResponseDto created = practiceQuestionService.create(practiceContentId, request);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Practice question created successfully", created));
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

    @GetMapping("/{practiceContentId}")
    public ResponseEntity<ApiResponse<List<PracticeQuestionResponseDto>>> getAllByPracticeContentId(
            @PathVariable String practiceContentId,
            @RequestParam(required = false) String include
    ) {
        try {
            IncludeSpec includes = IncludeParser.parse(include);

            List<PracticeQuestionResponseDto> questions =
                    practiceQuestionService.getAllByPracticeContentId(practiceContentId, includes);

            return ResponseEntity.ok(
                    ApiResponse.success("Practice questions fetched successfully", questions)
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
    public ResponseEntity<ApiResponse<PracticeQuestionResponseDto>> updatePracticeQuestion(
            @PathVariable String id,
            @Valid @RequestBody PracticeQuestionUpdateRequestDto request
    ) {
        try {
            PracticeQuestionResponseDto updated = practiceQuestionService.update(id, request);
            return ResponseEntity.ok(
                    ApiResponse.success("Practice question updated successfully", updated)
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
    public ResponseEntity<ApiResponse<String>> deletePracticeQuestion(
            @PathVariable String id
    ) {
        try {
            practiceQuestionService.delete(id);
            return ResponseEntity.ok(
                    ApiResponse.success("Practice question deleted successfully", null)
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
