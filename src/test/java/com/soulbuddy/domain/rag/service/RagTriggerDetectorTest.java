package com.soulbuddy.domain.rag.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PR-6 RagTriggerDetector 검증.
 *  - 키워드 hit → true
 *  - 키워드 미포함 → false
 *  - null / blank → false
 */
class RagTriggerDetectorTest {

    private RagTriggerDetector detector;

    @BeforeEach
    void setUp() {
        detector = new RagTriggerDetector("기억나,지난번,예전에,말했던,얘기했던,저번에,그때,예전,지난");
        ReflectionTestUtils.invokeMethod(detector, "init");
    }

    @Test
    @DisplayName("키워드 hit — '기억나' 포함 발화는 트리거 hit")
    void triggered_byKeyword() {
        assertThat(detector.isTriggered("저번에 학교 얘기했던 거 기억나?")).isTrue();
        assertThat(detector.isTriggered("그때 그 친구 어떻게 됐어?")).isTrue();
        assertThat(detector.isTriggered("예전에 시험 압박 얘기했던 거")).isTrue();
    }

    @Test
    @DisplayName("키워드 미포함 — 일반 발화는 트리거 안 됨")
    void notTriggered_withoutKeyword() {
        assertThat(detector.isTriggered("오늘 기분이 좀 안 좋아")).isFalse();
        assertThat(detector.isTriggered("학교 가기 싫어")).isFalse();
    }

    @Test
    @DisplayName("null / blank — false")
    void notTriggered_onNullOrBlank() {
        assertThat(detector.isTriggered(null)).isFalse();
        assertThat(detector.isTriggered("")).isFalse();
        assertThat(detector.isTriggered("   ")).isFalse();
    }
}
