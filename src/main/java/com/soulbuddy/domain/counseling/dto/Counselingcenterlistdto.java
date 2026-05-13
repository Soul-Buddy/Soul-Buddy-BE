package com.soulbuddy.domain.counseling.dto;

import com.soulbuddy.domain.counseling.entity.CounselingCenter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Schema(description = "상담센터 정보 응답")
public class Counselingcenterlistdto {

    @Schema(description = "센터 ID", example = "1")
    private Long id;

    @Schema(description = "센터 이름", example = "마음돌봄 상담센터")
    private String name;

    @Schema(description = "전화번호 (FE에서 tel: 링크로 사용)", example = "033-123-4567")
    private String phone;

    @Schema(description = "지역", example = "원주")
    private String region;

    @Schema(description = "긴급 여부 (상단 빨간 버튼용 데이터)", example = "true")
    private Boolean isEmergency;

    public static Counselingcenterlistdto fromEntity(CounselingCenter entity) {
        return Counselingcenterlistdto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .phone(entity.getPhone())
                .region(entity.getRegion())
                .isEmergency(entity.getIsEmergency())
                .build();
    }
}