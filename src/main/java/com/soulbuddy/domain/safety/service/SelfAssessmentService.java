package com.soulbuddy.domain.safety.service;

import com.soulbuddy.domain.safety.dto.SelfAssessmentDto;
import com.soulbuddy.domain.safety.entity.SelfAssessment;
import com.soulbuddy.domain.safety.repository.SelfAssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
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

        // 1. DTO의 점수 데이터들을 엔티티의 Map<String, String> answers에 담기
        Map<String, String> answerMap = new HashMap<>();
        answerMap.put("totalScore", String.valueOf(request.getTotalScore()));
        answerMap.put("suicidalThoughtScore", String.valueOf(request.getSuicidalThoughtScore()));
        answerMap.put("selfHarmIntentScore", String.valueOf(request.getSelfHarmIntentScore()));
        answerMap.put("hopelessnessScore", String.valueOf(request.getHopelessnessScore()));
        answerMap.put("riskLevel", request.getRiskLevel());
        answerMap.put("notes", request.getNotes());
        answerMap.put("rawResponse", request.getResponseData());

        // 2. 엔티티 생성 및 저장
        SelfAssessment entity = SelfAssessment.builder()
                .userId(request.getUserId())
                .sessionId(request.getSessionId())
                .assessmentType("SUICIDE_RISK") // 기본값
                .answers(answerMap)
                .build();

        SelfAssessment saved = selfAssessmentRepository.save(entity);

        return toResponse(saved);
    }

    /** 유저별 설문 이력 조회 */
    @Transactional(readOnly = true)
    public List<SelfAssessmentDto.Response> getAssessmentsByUser(Long userId) {
        return selfAssessmentRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** 세션별 설문 조회 */
    @Transactional(readOnly = true)
    public List<SelfAssessmentDto.Response> getAssessmentsBySession(String sessionId) {
        return selfAssessmentRepository
                .findBySessionId(sessionId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────
    // Private 헬퍼: Map에서 데이터를 꺼내 DTO로 변환
    // ──────────────────────────────────

    private SelfAssessmentDto.Response toResponse(SelfAssessment entity) {
        Map<String, String> answers = entity.getAnswers();

        return SelfAssessmentDto.Response.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .sessionId(entity.getSessionId())
                // Map에 저장된 String 값을 다시 Integer 등으로 변환하여 DTO에 매핑
                .totalScore(parseSafeInt(answers.get("totalScore")))
                .suicidalThoughtScore(parseSafeInt(answers.get("suicidalThoughtScore")))
                .selfHarmIntentScore(parseSafeInt(answers.get("selfHarmIntentScore")))
                .hopelessnessScore(parseSafeInt(answers.get("hopelessnessScore")))
                .riskLevel(answers.get("riskLevel"))
                .notes(answers.get("notes"))
                .responseData(answers.get("rawResponse"))
                .isCompleted(true)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private Integer parseSafeInt(String value) {
        try {
            return (value != null) ? Integer.parseInt(value) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
