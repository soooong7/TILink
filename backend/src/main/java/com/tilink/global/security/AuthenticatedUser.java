package com.tilink.global.security;

/**
 * 토큰에서 복원한 인증 주체. {@code Authentication#getPrincipal()} 로 꺼내 쓴다.
 *
 * @param id    사용자 ID(UUID 문자열)
 * @param email 사용자 이메일
 */
public record AuthenticatedUser(String id, String email) {}
