package com.soulbuddy.be.domain.safety.controller;

import com.soulbuddy.be.domain.safety.dto.SelfAssessmentDto;
import com.soulbuddy.be.domain.safety.service.SelfAssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Self Assessment API", description = "자살 위험성 자가설문 조회 및 제출을 위한 API")
@RestController
@RequestMapping("/api/safety/self-assessments")
@RequiredArgsConstructor
public class SelfAssessmentController {

    private final SelfAssessmentService selfAssessmentService;

    /**
     * POST /api/safety/self-assessments
     * 자살 위험성 자가설문 제출
     */
    @Operation(summary = "자가설문 제출", description = "사용자가 작성한 자살 위험성 자가설문을 제출합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "설문 제출 성공"),
            @ApiResponse(responseCode = "400", description = "요청 데이터 유효성 검사 실패", content = @Content),
            @ApiResponse(responseCode = "404", description = "해당 사용자를 찾을 수 없음", content = @Content)
    })
    @PostMapping
    public ResponseEntity<SelfAssessmentDto.Response> submit(
            @RequestBody @Valid SelfAssessmentDto.Request request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(selfAssessmentService.submitAssessment(request));
    }

    /**
     * GET /api/safety/self-assessments/user/{userId}
     * 유저별 설문 이력 조회
     */
    @Operation(summary = "사용자 설문 이력 조회", description = "특정 사용자가 작성한 모든 자가설문 이력을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 사용자를 찾을 수 없음", content = @Content)
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SelfAssessmentDto.Response>> getByUser(
            @Parameter(description = "사용자 ID", required = true, example = "123")
            @PathVariable Long userId) {

        return ResponseEntity.ok(selfAssessmentService.getAssessmentsByUser(userId));
    }

    /**
     * GET /api/safety/self-assessments/session/{sessionId}
     * 세션별 설문 조회
     */
    @Operation(summary = "세션별 설문 조회", description = "특정 상담 세션에서 작성된 자가설문을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 세션을 찾을 수 없음", content = @Content)
    })
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<SelfAssessmentDto.Response>> getBySession(
            @Parameter(description = "세션 ID", required = true, example = "SESSION-001")
            @PathVariable String sessionId) {

        return ResponseEntity.ok(selfAssessmentService.getAssessmentsBySession(sessionId));
    }
}