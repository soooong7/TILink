package com.tilink.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml 의 {@code jwt.*} 설정.
 *
 * @param secret           HMAC 서명 키. 최소 32바이트여야 한다. 절대 코드에 하드코딩하지 않고
 *                         환경변수({@code JWT_SECRET})로 주입한다.
 * @param expirationMinutes 액세스 토큰 만료 시간(분)
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, long expirationMinutes) {}
