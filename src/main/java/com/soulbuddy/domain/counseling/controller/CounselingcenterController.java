package com.soulbuddy.domain.counseling.controller;

import com.soulbuddy.domain.counseling.dto.Counselingcenterlistdto;
import com.soulbuddy.domain.counseling.service.Counselingcenterservice;
import com.soulbuddy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Counseling", description = "상담센터 정보 조회 API")
@RestController
@RequestMapping("/api/counseling-centers")
@RequiredArgsConstructor
public class CounselingcenterController {

    private final Counselingcenterservice counselingcenterservice;

    @Operation(summary = "상담센터 목록 조회", description = "긴급 상담 전화 우선 노출 및 지역 필터링 기능을 제공합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Counselingcenterlistdto>>> getCenters(
            @Parameter(description = "지역명 (예: 서울, 원주 등)", example = "원주")
            @RequestParam(required = false) String region,
            @Parameter(description = "긴급 상담 전화만 보기", example = "false")
            @RequestParam(defaultValue = "false") Boolean emergencyOnly) {

        List<Counselingcenterlistdto> data = counselingcenterservice.getCenters(region, emergencyOnly);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}