package com.soulbuddy.be.domain.user.service;

import com.soulbuddy.be.domain.user.dto.OnboardingRequest;
import com.soulbuddy.be.domain.user.entity.Profile;
import com.soulbuddy.be.domain.user.entity.User;
import com.soulbuddy.be.domain.user.repository.ProfileRepository;
import com.soulbuddy.be.domain.user.repository.UserRepository;
import com.soulbuddy.be.global.exception.BusinessException;
import com.soulbuddy.be.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    @Transactional
    public Profile createProfile(Long userId, OnboardingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        Profile profile = Profile.builder()
                .user(user)
                .nickname(request.getNickname())
                .hobbies(request.getHobbies())
                .personality(request.getPersonality())
                .concerns(request.getConcerns())
                .preferredTone(request.getPreferredTone())
                .likedThings(request.getLikedThings())
                .dislikedThings(request.getDislikedThings())
                .additionalInfo(request.getAdditionalInfo())
                .build();

        return profileRepository.save(profile);
    }

    public Profile getProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_001));
    }

    @Transactional
    public Profile updateProfile(Long userId, OnboardingRequest request) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_001));

        Profile updated = Profile.builder()
                .id(profile.getId())
                .user(profile.getUser())
                .nickname(request.getNickname())
                .hobbies(request.getHobbies())
                .personality(request.getPersonality())
                .concerns(request.getConcerns())
                .preferredTone(request.getPreferredTone())
                .likedThings(request.getLikedThings())
                .dislikedThings(request.getDislikedThings())
                .additionalInfo(request.getAdditionalInfo())
                .build();

        return profileRepository.save(updated);
    }

    public boolean hasProfile(Long userId) {
        return profileRepository.existsByUserId(userId);
    }

    /**
     * [추가] 세션 생성·채팅 시 닉네임 조회
     */
    public String getNicknameByUserId(Long userId) {
        return profileRepository.findByUserId(userId)
                .map(Profile::getNickname)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_001));
    }

    /**
     * [추가] 채팅 시 PromptContext 조립 (개인화 지침 생성)
     */
    public String getPersonalInstructionByUserId(Long userId) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_001));

        // AI 모델에 전달할 사용자의 성향 및 정보를 하나의 문자열로 구성합니다.
        return String.format(
                "사용자 정보 - 닉네임: %s, 성격: %s, 취미: %s, 고민거리: %s, 선호하는 말투: %s, 좋아하는 것: %s, 싫어하는 것: %s, 기타 사항: %s",
                profile.getNickname(),
                profile.getPersonality(),
                profile.getHobbies(),
                profile.getConcerns(),
                profile.getPreferredTone(),
                profile.getLikedThings(),
                profile.getDislikedThings(),
                profile.getAdditionalInfo()
        );
    }
}