package com.soulbuddy.ai.dto;

import com.soulbuddy.global.enums.EmotionTag;
import com.soulbuddy.global.enums.InterventionType;
import com.soulbuddy.global.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private String assistantMessage;
    private EmotionTag emotionTag;
    private RiskLevel riskLevel;
    private InterventionType interventionType;
    private boolean ragUsed;
    private String aiModel;
    private boolean forcedSafety;
    private String summary;
    private String memoryHint;
    private String recommendedAction;

    /**
     * v2.3 — 강제 안전 발화 시 FE 가 "상담센터 이동 / 대화 이어가기" 선택 UI 노출 신호.
     * forcedSafety=true 인 응답에서만 true 로 설정된다.
     */
    private boolean showSafetyChoice;

    /**
     * v2.3 — showSafetyChoice=true 일 때 FE 가 라우팅할 상담센터 경로.
     * application.yml soulbuddy.safety.counseling-center-path 값.
     */
    private String counselingCenterPath;
}
