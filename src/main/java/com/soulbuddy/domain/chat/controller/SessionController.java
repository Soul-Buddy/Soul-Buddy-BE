package com.soulbuddy.domain.chat.controller;

import com.soulbuddy.domain.chat.dto.request.PreChatEmotionRequest;
import com.soulbuddy.domain.chat.dto.request.SessionCreateRequest;
import com.soulbuddy.domain.chat.dto.response.PreChatEmotionResponse;
import com.soulbuddy.domain.chat.dto.response.SessionCreateResponse;
import com.soulbuddy.domain.chat.dto.response.SessionDeleteResponse;
import com.soulbuddy.domain.chat.dto.response.SessionEndResponse;
import com.soulbuddy.domain.chat.dto.response.SessionListResponse;
import com.soulbuddy.domain.chat.service.SessionService;
import com.soulbuddy.domain.user.service.ProfileQueryService;
import com.soulbuddy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@Tag(name = "Session", description = "채팅 세션 관리 API")
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;
    private final ProfileQueryService profileQueryService;

    @Operation(
            summary = "세션 목록 조회",
            description = "현재 사용자의 채팅 세션 목록을 조회합니다. 상태(ONGOING/ENDED) 필터 및 페이지네이션을 지원합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<SessionListResponse>> getSessions(
            @Parameter(hidden = true) @AuthenticationPrincipal String principal,
            @Parameter(description = "세션 상태 필터 (ONGOING | ENDED). 미입력 시 전체 조회") @RequestParam(required = false) String status,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {

        Long userId = Long.parseLong(principal);

        return ResponseEntity.ok(ApiResponse.success(
                sessionService.getSessions(userId, status, page, size)
        ));
    }

    @Operation(
            summary = "세션 생성",
            description = "새 채팅 세션을 생성합니다. 이전 세션의 memoryHint를 recentSummary로 반환하며, 이 값을 이후 채팅 요청 시 함께 전송해야 합니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<SessionCreateResponse>> createSession(
            @Parameter(hidden = true) @AuthenticationPrincipal String principal,
            @Valid @RequestBody SessionCreateRequest request) {

        Long userId = Long.parseLong(principal);
        String nickname = profileQueryService.getNicknameByUserId(userId);

        return ResponseEntity.ok(ApiResponse.success(
                sessionService.createSession(userId, request.getPersonaType(), nickname)
        ));
    }

    @Operation(
            summary = "세션 삭제",
            description = "세션을 소프트 삭제합니다. 삭제된 세션은 목록에서 제외됩니다."
    )
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<SessionDeleteResponse>> deleteSession(
            @Parameter(hidden = true) @AuthenticationPrincipal String principal,
            @Parameter(description = "삭제할 세션 ID (UUID)") @PathVariable String sessionId) {

        Long userId = Long.parseLong(principal);

        return ResponseEntity.ok(ApiResponse.success(
                sessionService.deleteSession(userId, sessionId)
        ));
    }

    @Operation(
            summary = "세션 종료",
            description = "세션을 종료하고 AI가 대화 내용을 분석하여 요약 카드를 생성합니다."
    )
    @PatchMapping("/{sessionId}/end")
    public ResponseEntity<ApiResponse<SessionEndResponse>> endSession(
            @Parameter(hidden = true) @AuthenticationPrincipal String principal,
            @Parameter(description = "종료할 세션 ID (UUID)") @PathVariable String sessionId) {

        Long userId = Long.parseLong(principal);

        return ResponseEntity.ok(ApiResponse.success(
                sessionService.endSession(userId, sessionId)
        ));
    }

    @Operation(
            summary = "채팅 전 감정 선택",
            description = "채팅 시작 전 현재 감정 상태를 기록합니다. 대시보드 감정 통계에 반영됩니다."
    )
    @PatchMapping("/{sessionId}/pre-chat-emotion")
    public ResponseEntity<ApiResponse<PreChatEmotionResponse>> updatePreChatEmotion(
            @Parameter(hidden = true) @AuthenticationPrincipal String principal,
            @Parameter(description = "세션 ID (UUID)") @PathVariable String sessionId,
            @Valid @RequestBody PreChatEmotionRequest request) {

        Long userId = Long.parseLong(principal);

        return ResponseEntity.ok(ApiResponse.success(
                sessionService.updatePreChatEmotion(userId, sessionId, request.getPreChatEmotion())
        ));
    }
}
