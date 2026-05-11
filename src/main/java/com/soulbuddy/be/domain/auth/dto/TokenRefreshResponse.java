package com.soulbuddy.be.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenRefreshResponse {

    private String accessToken;
}
