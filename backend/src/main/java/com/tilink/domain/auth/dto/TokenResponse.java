package com.tilink.domain.auth.dto;

/**
 * 로그인 응답.
 *
 * @param accessToken JWT 액세스 토큰
 * @param tokenType   항상 {@code "Bearer"}
 * @param expiresIn   만료까지 남은 시간(초)
 */
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {

    private static final String BEARER = "Bearer";

    public static TokenResponse of(String accessToken, long expiresIn) {
        return new TokenResponse(accessToken, BEARER, expiresIn);
    }
}
