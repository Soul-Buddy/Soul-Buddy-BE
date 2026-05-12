package com.soulbuddy;

import com.soulbuddy.global.auth.JwtTokenProvider;

public class TokenGenerator {

    public static void main(String[] args) {
        JwtTokenProvider provider = new JwtTokenProvider(
                "local-dev-secret-key-must-be-at-least-32-chars-soul-buddy-2026",
                86400000L,
                604800000L
        );
        System.out.println("AccessToken: " + provider.createAccessToken(1L));
    }
}
