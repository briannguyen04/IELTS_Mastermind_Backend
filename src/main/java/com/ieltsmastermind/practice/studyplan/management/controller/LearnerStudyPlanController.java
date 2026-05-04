package com.ieltsmastermind.practice.studyplan.management.controller;

import com.ieltsmastermind.common.query.IncludeParser;
import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.common.response.ApiResponse;
import com.ieltsmastermind.practice.content.management.domain.enums.PracticeContentSkill;
import com.ieltsmastermind.practice.studyplan.management.business.interfaces.LearnerStudyPlanService;
import com.ieltsmastermind.practice.studyplan.management.domain.dto.LearnerStudyPlanActiveCheckResponseDto;
import com.ieltsmastermind.practice.studyplan.management.domain.dto.LearnerStudyPlanCreateRequestDto;
import com.ieltsmastermind.practice.studyplan.management.domain.dto.LearnerStudyPlanRefreshAfterSubmissionResponseDto;
import com.ieltsmastermind.practice.studyplan.management.domain.dto.LearnerStudyPlanResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/learner-study-plan")
@RequiredArgsConstructor
public class LearnerStudyPlanController {

    private final LearnerStudyPlanService learnerStudyPlanService;

    @PostMapping
    public ResponseEntity<ApiResponse<LearnerStudyPlanResponseDto>> create(
            @Valid @RequestBody LearnerStudyPlanCreateRequestDto request
    ) {
        try {
            LearnerStudyPlanResponseDto created = learnerStudyPlanService.create(request);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Learner study plan created successfully", created));
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

    @GetMapping("/user/{userId}/skill/{skill}/active")
    public ResponseEntity<ApiResponse<LearnerStudyPlanResponseDto>> getActiveByUserIdAndSkill(
            @PathVariable String userId,
            @PathVariable PracticeContentSkill skill,
            @RequestParam(required = false) String include
    ) {
        try {
            IncludeSpec includes = IncludeParser.parse(include);

            LearnerStudyPlanResponseDto studyPlan =
                    learnerStudyPlanService.getActiveStudyPlanByUserIdAndSkill(userId, skill, includes);

            return ResponseEntity.ok(
                    ApiResponse.success("Active study plan fetched successfully", studyPlan)
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

    @GetMapping("/user/{userId}/skill/{skill}/active/check")
    public ResponseEntity<ApiResponse<LearnerStudyPlanActiveCheckResponseDto>> checkHasActiveStudyPlan(
            @PathVariable String userId,
            @PathVariable PracticeContentSkill skill
    ) {
        try {
            LearnerStudyPlanActiveCheckResponseDto response =
                    learnerStudyPlanService.getHasActiveStudyPlan(userId, skill);

            return ResponseEntity.ok(
                    ApiResponse.success("Active learner study plan status fetched successfully", response)
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

    @PutMapping("/{id}/finalize")
    public ResponseEntity<ApiResponse<LearnerStudyPlanResponseDto>> finalizeStudyPlan(
            @PathVariable String id
    ) {
        try {
            LearnerStudyPlanResponseDto finalized = learnerStudyPlanService.finalizeStudyPlanById(id);
            return ResponseEntity
                    .ok(ApiResponse.success("Learner study plan finalized successfully", finalized));
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

    @PutMapping("/user/{userId}/skill/{skill}/refresh")
    public ResponseEntity<ApiResponse<LearnerStudyPlanResponseDto>> refreshStudyPlan(
            @PathVariable String userId,
            @PathVariable PracticeContentSkill skill
    ) {
        try {
            LearnerStudyPlanResponseDto refreshed =
                    learnerStudyPlanService.refreshStudyPlan(userId, skill);

            return ResponseEntity.ok(
                    ApiResponse.success(
                            "Learner analytics and study plan refreshed successfully",
                            refreshed
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
