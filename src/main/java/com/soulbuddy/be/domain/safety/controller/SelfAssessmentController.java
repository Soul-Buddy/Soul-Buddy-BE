// com/soulbuddy/be/domain/safety/controller/SelfAssessmentController.java
package com.soulbuddy.be.domain.safety.controller;

import com.soulbuddy.be.domain.safety.dto.SelfAssessmentDto;
import com.soulbuddy.be.domain.safety.service.SelfAssessmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/safety/self-assessments")
@RequiredArgsConstructor
public class SelfAssessmentController {

    private final SelfAssessmentService selfAssessmentService;

    /**
     * POST /api/safety/self-assessments
     * 자살 위험성 자가설문 제출
     */
    @PostMapping
    public ResponseEntity<SelfAssessmentDto.Response> submit(
            @RequestBody @Valid SelfAssessmentDto.Request request) {

        return ResponseEntity.ok(selfAssessmentService.submitAssessment(request));
    }

    /**
     * GET /api/safety/self-assessments/user/{userId}
     * 유저별 설문 이력 조회
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SelfAssessmentDto.Response>> getByUser(
            @PathVariable Long userId) {

        return ResponseEntity.ok(selfAssessmentService.getAssessmentsByUser(userId));
    }

    /**
     * GET /api/safety/self-assessments/session/{sessionId}
     * 세션별 설문 조회
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<SelfAssessmentDto.Response>> getBySession(
            @PathVariable String sessionId) {

        return ResponseEntity.ok(selfAssessmentService.getAssessmentsBySession(sessionId));
    }
}
