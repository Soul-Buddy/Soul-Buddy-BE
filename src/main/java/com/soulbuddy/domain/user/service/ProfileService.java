package com.soulbuddy.domain.user.service;

import com.soulbuddy.domain.user.dto.*;
import com.soulbuddy.domain.user.entity.Profile;
import com.soulbuddy.domain.user.entity.User;
import com.soulbuddy.domain.user.entity.UserSettings;
import com.soulbuddy.domain.user.repository.ProfileRepository;
import com.soulbuddy.domain.user.repository.UserRepository;
import com.soulbuddy.domain.user.repository.UserSettingsRepository;
import com.soulbuddy.global.enums.Gender;         // Gender 임포트
import com.soulbuddy.global.enums.PreferredTone;  // PreferredTone 임포트
import com.soulbuddy.global.exception.BusinessException;
import com.soulbuddy.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileService implements ProfileQueryService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;

    @Override
    @Transactional(readOnly = true)
    public String getNicknameByUserId(Long userId) {
        return profileRepository.findByUserId(userId).map(Profile::getNickname).orElse("사용자");
    }

    @Override
    @Transactional(readOnly = true)
    public String getPersonalInstructionByUserId(Long userId) {
        return profileRepository.findByUserId(userId).map(Profile::getPersonalInstruction).orElse("");
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_001));
        return ProfileResponse.from(profile);
    }

    @Transactional
    public OnboardingResponse onboard(Long userId, OnboardingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

        String instruction = createInstruction(
                request.nickname(), request.age(), request.gender(), request.occupation(),
                request.usageIntent(), request.hobbies(), request.preferredTone(),
                request.likedThings(), request.dislikedThings()
        );

        // String -> Enum 변환 (오류 해결 핵심)
        Gender genderEnum = Gender.valueOf(request.gender().toUpperCase());
        PreferredTone toneEnum = PreferredTone.valueOf(request.preferredTone().toUpperCase());

        Profile profile = profileRepository.findByUserId(userId).orElse(null);

        if (profile == null) {
            profile = Profile.builder()
                    .user(user)
                    .nickname(request.nickname())
                    .age(request.age())
                    .gender(genderEnum)
                    .occupation(request.occupation())
                    .usageIntent(request.usageIntent())
                    .hobbies(request.hobbies())
                    .preferredTone(toneEnum)
                    .likedThings(request.likedThings())
                    .dislikedThings(request.dislikedThings())
                    .personalInstruction(instruction)
                    .build();
            profile.completeOnboarding();
            profile = profileRepository.save(profile);
        } else {
            profile.update(request.nickname(), request.age(), genderEnum,
                    request.occupation(), request.usageIntent(), request.hobbies(),
                    toneEnum, request.likedThings(), request.dislikedThings());
            profile.updateInstruction(instruction);
            profile.completeOnboarding();
        }

        userSettingsRepository.findByUserId(userId)
                .orElseGet(() -> userSettingsRepository.save(UserSettings.defaultFor(user)));

        return OnboardingResponse.of(profile.getId(), profile.getOnboardingCompletedAt());
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_001));

        String instruction = createInstruction(
                request.nickname(), request.age(), request.gender(), request.occupation(),
                request.usageIntent(), request.hobbies(), request.preferredTone(),
                request.likedThings(), request.dislikedThings()
        );

        Gender genderEnum = Gender.valueOf(request.gender().toUpperCase());
        PreferredTone toneEnum = PreferredTone.valueOf(request.preferredTone().toUpperCase());

        profile.update(request.nickname(), request.age(), genderEnum,
                request.occupation(), request.usageIntent(), request.hobbies(),
                toneEnum, request.likedThings(), request.dislikedThings());

        profile.updateInstruction(instruction);

        return ProfileResponse.from(profile);
    }

    private String createInstruction(String nickname, int age, String gender, String occupation,
                                     String usageIntent, List<String> hobbies, String preferredTone,
                                     List<String> likedThings, List<String> dislikedThings) {

        String hobbyPart = (hobbies != null && !hobbies.isEmpty()) ? String.join(", ", hobbies) : "없음";
        String likedPart = (likedThings != null && !likedThings.isEmpty()) ? String.join(", ", likedThings) : "없음";
        String dislikePart = (dislikedThings != null && !dislikedThings.isEmpty()) ? String.join(", ", dislikedThings) : "없음";
        String tonePart = "FRIEND".equals(preferredTone) ? "친근하고 편안한 친구" : "차분하고 전문적인 상담사";

        return String.format(
                "사용자의 이름은 %s이며, %d세(%s) %s입니다. 이용 목적은 '%s'입니다. " +
                        "관심사 및 취미는 [%s]이며, 좋아하는 것은 [%s], 싫어하는 것은 [%s]입니다. " +
                        "이 사용자는 %s 같은 말투로 대화하는 것을 선호합니다.",
                nickname, age, gender,
                (occupation != null && !occupation.isBlank()) ? occupation : "미정",
                usageIntent, hobbyPart, likedPart, dislikePart, tonePart
        );
    }
}