package com.soulbuddy.ai.filter;

import com.soulbuddy.global.enums.RiskLevel;
import org.springframework.stereotype.Component;

/**
 * Safety Gate.
 *
 * v2.3 정합 — immediateHighRisk(키워드 직격 차단) 정책 폐기.
 * 분류기 HIGH 결과만 인정하며, 누적 임계치 도달 시 강제 안전 발화로 응답을 교체한다.
 *
 * 1회 발화 여부 판정은 SafetyEventService.hasForcedSafetyEmitted(sessionId) 로
 * safety_events 테이블을 조회하여 결정한다 (AiChatServiceImpl 에서 수행).
 *
 * 강제 안전 발화 본문/추천 액션은 system_prompts_final.txt 공통 규칙 5번을 따른다.
 */
@Component
public class SafetyFilter {

    public static final String SAFETY_MESSAGE =
            "지금 많이 힘드시겠어요. 혼자 감당하기 어려운 순간엔 전문가와 이야기 나눠보는 것이 " +
            "큰 도움이 될 수 있어요. 자살예방 상담전화 109(24시간), 청소년상담 1388, " +
            "정신건강 위기상담 1577-0199로 언제든 연락하실 수 있어요.";

    public static final String SAFETY_ACTION =
            "자살예방 상담전화 109 (24시간 운영)";

    /**
     * 강제 안전 발화로 일반 응답을 차단할지 결정.
     * - 분류기 HIGH + 최근 HIGH 누적 횟수+1 ≥ threshold → true
     *
     * ※ "같은 세션에 FORCED_SAFETY 이미 발화했는지" 검사는 호출부(AiChatServiceImpl)에서
     *   safety_events 조회로 수행하고, 발화 이력 있으면 강제 안전 발화를 재출력하지 않는다.
     */
    public boolean decideForcedSafety(RiskLevel classifiedRisk,
                                      long recentHighCount, int threshold) {
        if (classifiedRisk == RiskLevel.HIGH && recentHighCount + 1 >= threshold) {
            return true;
        }
        return false;
    }
}
