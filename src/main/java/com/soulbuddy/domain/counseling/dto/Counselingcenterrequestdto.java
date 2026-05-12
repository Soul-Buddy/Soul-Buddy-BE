package com.soulbuddy.domain.counseling.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "상담센터 생성/수정 요청 DTO")
public class Counselingcenterrequestdto {

    @NotBlank(message = "상담센터 이름은 필수입니다")
    @Size(min = 1, max = 255, message = "이름은 1자 이상 255자 이하여야 합니다")
    @Schema(description = "센터명", example = "청소년 상담센터", required = true)
    private String name;

    @NotBlank(message = "전화번호는 필수입니다")
    @Size(min = 1, max = 50, message = "전화번호는 1자 이상 50자 이하여야 합니다")
    @Schema(description = "전화번호", example = "02-1234-5678", required = true)
    private String phone;

    @Size(max = 100, message = "지역은 100자 이하여야 합니다")
    @Schema(description = "지역", example = "서울")
    private String region;

    @Schema(description = "24시간 긴급 상담 센터 여부", example = "false")
    private Boolean isEmergency;

    @Schema(description = "센터 활성화 여부", example = "true")
    private Boolean isActive;

    @Schema(description = "정렬 순서", example = "1")
    private Integer sortOrder;
}

