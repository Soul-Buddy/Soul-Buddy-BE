package com.soulbuddy.domain.auth.service;

import com.soulbuddy.domain.auth.dto.AgreementRequest;
import com.soulbuddy.domain.auth.dto.AgreementResponse;
import com.soulbuddy.domain.user.entity.User;
import com.soulbuddy.domain.user.repository.UserRepository;
import com.soulbuddy.global.exception.BusinessException; // 전역 예외 처리가 있다면 교체 권장
import com.soulbuddy.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AgreementService {

    private final UserRepository userRepository;

    @Transactional
    public AgreementResponse agree(Long userId, AgreementRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("USER_001: 존재하지 않는 사용자입니다."));

        // DTO에서 @AssertTrue로 검증하지만, 서비스 단에서도 안전하게 체크 (선택사항)
        if (!request.termsAgreed() || !request.privacyAgreed()) {
            throw new IllegalArgumentException("VALID_001: 필수 약관 동의가 누락되었습니다.");
        }

        // 이미 동의한 경우 날짜 업데이트 없이 기존 정보 반환
        if (user.getTermsAgreedAt() != null) {
            return new AgreementResponse(user.getTermsAgreedAt(), user.getPrivacyAgreedAt());
        }

        // 동의 처리
        LocalDateTime now = LocalDateTime.now();
        user.agreeTerms(now);
        user.agreePrivacy(now);

        // Dirty Checking 덕분에 userRepository.save(user)는 생략 가능하지만 명시해도 무방합니다.

        return new AgreementResponse(user.getTermsAgreedAt(), user.getPrivacyAgreedAt());
    }

    @Transactional(readOnly = true)
    public AgreementResponse getAgreementStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("USER_001: 존재하지 않는 사용자입니다."));

        return new AgreementResponse(user.getTermsAgreedAt(), user.getPrivacyAgreedAt());
    }
}