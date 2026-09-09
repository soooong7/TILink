package com.tilink.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

/**
 * 액세스 토큰 발급·검증 담당.
 *
 * <p>서명 알고리즘은 jjwt 가 키 길이에 맞춰 고른다(32바이트=HS256, 48바이트=HS384, 64바이트=HS512).
 *
 * <p>클레임 구성: {@code sub}=사용자 ID(UUID), {@code email}, {@code iat}, {@code exp}.
 * 리프레시 토큰은 이번 단계 범위가 아니다.
 */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_EMAIL = "email";
    // HMAC-SHA 서명 키의 최소 길이. RFC 7518 이 256비트 이상을 요구한다.
    private static final int MINIMUM_SECRET_BYTES = 32;

    private final SecretKey key;
    private final Duration expiration;

    public JwtTokenProvider(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(validateSecret(properties.secret()));
        this.expiration = Duration.ofMinutes(properties.expirationMinutes());
    }

    /**
     * 서명 키를 검증한다. 문제가 있으면 기동 시점에 바로 실패시킨다.
     *
     * <p>@ConfigurationProperties 바인딩은 해석하지 못한 플레이스홀더를 예외 없이
     * "${JWT_SECRET}" 이라는 문자열 그대로 넘긴다. 그대로 두면 환경변수를 빠뜨린 채
     * 예측 가능한 값으로 토큰을 서명하게 되므로 여기서 직접 막는다.
     */
    private static byte[] validateSecret(String secret) {
        if (secret == null || secret.isBlank() || secret.startsWith("${")) {
            throw new IllegalStateException(
                    "JWT_SECRET 환경변수가 설정되지 않았습니다. 32바이트 이상의 임의 문자열을 지정하세요. "
                            + "예: export JWT_SECRET=$(openssl rand -base64 48)");
        }

        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET 이 너무 짧습니다. 최소 %d 바이트가 필요하지만 %d 바이트입니다."
                            .formatted(MINIMUM_SECRET_BYTES, bytes.length));
        }
        return bytes;
    }

    // 사용자 ID/이메일을 담은 액세스 토큰을 발급한다.
    public String createAccessToken(String userId, String email) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId)
                .claim(CLAIM_EMAIL, email)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration.toMillis()))
                .signWith(key)
                .compact();
    }

    // 토큰 만료 시간(초). 로그인 응답에 그대로 내려준다.
    public long getExpiresInSeconds() {
        return expiration.toSeconds();
    }

    /**
     * 서명과 만료를 검증하고 인증 주체를 꺼낸다.
     *
     * <p>토큰이 유효하지 않으면 예외를 던지지 않고 빈 값을 반환한다. 인증 실패 응답은
     * 필터가 아니라 Security 의 {@code AuthenticationEntryPoint} 가 담당하기 때문이다.
     */
    public Optional<AuthenticatedUser> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new AuthenticatedUser(claims.getSubject(), claims.get(CLAIM_EMAIL, String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
