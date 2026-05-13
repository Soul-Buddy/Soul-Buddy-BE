package com.soulbuddy.domain.dashboard.controller;

import com.soulbuddy.domain.dashboard.dto.response.DashboardResponse;
import com.soulbuddy.domain.dashboard.service.DashboardService;
import com.soulbuddy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Dashboard", description = "감정 통계 및 요약 카드 API")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(
            summary = "대시보드 조회",
            description = "사용자의 감정 태그 통계와 세션 요약 카드 목록을 반환합니다."
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @Parameter(hidden = true) @AuthenticationPrincipal String principal,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Long userId = Long.parseLong(principal);

        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getDashboard(userId, page, size)
        ));
    }
}
