package com.soulbuddy.domain.chat.controller;

import com.soulbuddy.ai.dto.ChatRequest;
import com.soulbuddy.ai.dto.ChatResponse;
import com.soulbuddy.domain.chat.dto.response.ChatHistoryResponse;
import com.soulbuddy.domain.chat.service.ChatService;
import com.soulbuddy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Chat", description = "AI 채팅 메시지 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(
            summary = "메시지 전송",
            description = "AI에게 메시지를 전송하고 응답을 받습니다. 온보딩 정보(personalInstruction)가 AI 프롬프트에 자동 반영됩니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @Parameter(hidden = true) @AuthenticationPrincipal String principal,
            @Valid @RequestBody ChatRequest request) {

        Long userId = Long.parseLong(principal);

        return ResponseEntity.ok(ApiResponse.success(
                chatService.processChat(userId, request)
        ));
    }

    @Operation(
            summary = "대화 히스토리 조회",
            description = "특정 세션의 대화 내역을 페이지네이션으로 조회합니다."
    )
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<ApiResponse<ChatHistoryResponse>> getChatHistory(
            @Parameter(hidden = true) @AuthenticationPrincipal String principal,
            @Parameter(description = "세션 ID (UUID)") @PathVariable String sessionId,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "50") int size) {

        Long userId = Long.parseLong(principal);

        return ResponseEntity.ok(ApiResponse.success(
                chatService.getChatHistory(userId, sessionId, page, size)
        ));
    }
}
