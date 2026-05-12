// com/soulbuddy/be/domain/safety/service/SelfAssessmentService.java
package com.soulbuddy.be.domain.safety.service;

import com.soulbuddy.be.domain.safety.dto.SelfAssessmentDto;
import com.soulbuddy.be.domain.safety.entity.SelfAssessment;
import com.soulbuddy.be.domain.safety.repository.SelfAssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SelfAssessmentService {

    private final SelfAssessmentRepository selfAssessmentRepository;

    /** 자가설문 제출 및 저장 */
    @Transactional
    public SelfAssessmentDto.Response submitAssessment(SelfAssessmentDto.Request request) {

        SelfAssessment entity = SelfAssessment.builder()
                .userId(request.getUserId())
                .sessionId(request.getSessionId())
                .assessmentType(request.getAssessmentType())
                .answers(request.getAnswers())
                .build();

        SelfAssessment saved = selfAssessmentRepository.save(entity);

        int score = calculateScore(request.getAnswers());
        String riskGrade = determineRiskGrade(score);

        return toResponse(saved, score, riskGrade);
    }

    /** 유저별 설문 이력 조회 */
    @Transactional(readOnly = true)
    public List<SelfAssessmentDto.Response> getAssessmentsByUser(Long userId) {
        return selfAssessmentRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(e -> {
                    int score = calculateScore(e.getAnswers());
                    return toResponse(e, score, determineRiskGrade(score));
                })
                .collect(Collectors.toList());
    }

    /** 세션별 설문 조회 */
    @Transactional(readOnly = true)
    public List<SelfAssessmentDto.Response> getAssessmentsBySession(String sessionId) {
        return selfAssessmentRepository
                .findBySessionId(sessionId)
                .stream()
                .map(e -> {
                    int score = calculateScore(e.getAnswers());
                    return toResponse(e, score, determineRiskGrade(score));
                })
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────
    // Private 헬퍼
    // ──────────────────────────────────

    private int calculateScore(Map<String, String> answers) {
        int total = 0;
        for (String answer : answers.values()) {
            switch (answer) {
                case "NEVER"     -> total += 0;
                case "SOMETIMES" -> total += 1;
                case "OFTEN"     -> total += 2;
                default          -> total += 0;
            }
        }
        return total;
    }

    private String determineRiskGrade(int score) {
        if (score >= 4) return "HIGH";
        if (score >= 2) return "MEDIUM";
        return "LOW";
    }

    private SelfAssessmentDto.Response toResponse(SelfAssessment entity, int score, String riskGrade) {
        return SelfAssessmentDto.Response.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .sessionId(entity.getSessionId())
                .assessmentType(entity.getAssessmentType())
                .answers(entity.getAnswers())
                .createdAt(entity.getCreatedAt())
                .score(score)
                .riskGrade(riskGrade)
                .build();
    }
}
