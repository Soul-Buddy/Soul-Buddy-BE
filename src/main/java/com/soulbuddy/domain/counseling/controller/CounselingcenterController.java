package com.soulbuddy.domain.counseling.controller;

import com.soulbuddy.domain.counseling.dto.Counselingcenterrequestdto;
import com.soulbuddy.domain.counseling.dto.Counselingcenterresponsedto;
import com.soulbuddy.domain.counseling.dto.Counselingcenterlistdto;
import com.soulbuddy.domain.counseling.service.Counselingcenterservice;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "Counseling Center API", description = "상담센터 정보 조회 및 관리를 위한 API 목록")
@RestController
@RequestMapping("/api/v1/counseling-centers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class CounselingcenterController {

    private final Counselingcenterservice counselingcenterservice;

    @Operation(summary = "전체 상담센터 조회", description = "현재 운영 중인 모든 상담센터 목록을 반환합니다.")
    @GetMapping
    public ResponseEntity<?> getAllActiveCenters() {
        try {
            List<Counselingcenterlistdto> centers = counselingcenterservice.getAllActiveCenters();
            return ResponseEntity.ok(centers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("상담센터 목록 조회 실패: " + e.getMessage()));
        }
    }

    @Operation(summary = "지역별 상담센터 조회", description = "특정 지역 코드를 기반으로 상담센터를 조회합니다.")
    @GetMapping("/region")
    public ResponseEntity<?> getCentersByRegion(
            @Parameter(description = "지역명 (예: 서울, 경기, 부산)", required = true, example = "서울")
            @RequestParam String region) {
        try {
            List<Counselingcenterlistdto> centers = counselingcenterservice.getCentersByRegion(region);
            return ResponseEntity.ok(centers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("지역별 상담센터 조회 실패: " + e.getMessage()));
        }
    }

    @Operation(summary = "긴급 상담센터 조회", description = "24시간 운영되는 긴급 상담 및 위기 개입 센터를 조회합니다.")
    @GetMapping("/emergency")
    public ResponseEntity<?> getEmergencyCenters() {
        try {
            List<Counselingcenterlistdto> centers = counselingcenterservice.getEmergencyCenters();
            return ResponseEntity.ok(centers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("긴급 상담센터 조회 실패: " + e.getMessage()));
        }
    }

    @Operation(summary = "키워드 검색 조회", description = "상담센터 이름을 키워드로 검색합니다.")
    @GetMapping("/search")
    public ResponseEntity<?> searchCentersByName(
            @Parameter(description = "검색할 센터 이름", required = true, example = "청소년")
            @RequestParam String name) {
        try {
            List<Counselingcenterlistdto> centers = counselingcenterservice.searchCentersByName(name);
            return ResponseEntity.ok(centers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("상담센터 검색 실패: " + e.getMessage()));
        }
    }

    @Operation(summary = "상담센터 상세 정보 조회", description = "센터 고유 ID를 이용해 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 센터를 찾을 수 없음", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getCenterDetail(
            @Parameter(description = "센터 일련번호", required = true, example = "101")
            @PathVariable Long id) {
        try {
            Counselingcenterresponsedto center = counselingcenterservice.getCenterDetail(id);
            return ResponseEntity.ok(center);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("상담센터 조회 실패: " + e.getMessage()));
        }
    }

    @Operation(summary = "신규 센터 정보 등록", description = "새로운 상담센터 정보를 시스템에 추가합니다.")
    @PostMapping
    public ResponseEntity<?> createCenter(@Valid @RequestBody Counselingcenterrequestdto requestDto) {
        try {
            Counselingcenterresponsedto responseDto = counselingcenterservice.createCenter(requestDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("상담센터 생성 실패: " + e.getMessage()));
        }
    }

    @Operation(summary = "센터 정보 수정", description = "기존 센터의 상세 정보를 업데이트합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCenter(
            @Parameter(description = "수정할 센터 ID", required = true) @PathVariable Long id,
            @Valid @RequestBody Counselingcenterrequestdto requestDto) {
        try {
            Counselingcenterresponsedto responseDto = counselingcenterservice.updateCenter(id, requestDto);
            return ResponseEntity.ok(responseDto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("상담센터 수정 실패: " + e.getMessage()));
        }
    }

    @Operation(summary = "센터 정보 삭제", description = "시스템에서 해당 센터 정보를 완전히 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCenter(@PathVariable Long id) {
        try {
            counselingcenterservice.deleteCenter(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("상담센터 삭제 실패: " + e.getMessage()));
        }
    }

    @Operation(summary = "센터 비활성화", description = "데이터는 유지하되 사용자에게 노출되지 않도록 상태를 변경합니다.")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateCenter(@PathVariable Long id) {
        try {
            Counselingcenterresponsedto responseDto = counselingcenterservice.deactivateCenter(id);
            return ResponseEntity.ok(responseDto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("상담센터 비활성화 실패: " + e.getMessage()));
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}

