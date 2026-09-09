package com.tilink.global.security;

import tools.jackson.databind.json.JsonMapper;
import com.tilink.global.error.ErrorCode;
import com.tilink.global.error.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Security 필터 체인에서 발생하는 인증/인가 실패 응답.
 *
 * <p>이 지점은 {@code @RestControllerAdvice} 보다 앞단이라 전역 예외 핸들러가 잡지 못한다.
 * 그래서 같은 {@link ErrorResponse} 형태를 여기서 직접 JSON 으로 써 준다.
 */
@Component
@RequiredArgsConstructor
public class SecurityExceptionHandlers {

    private final JsonMapper jsonMapper;

    // 인증되지 않은 요청 → 401
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> write(response, ErrorCode.UNAUTHORIZED);
    }

    // 인증은 되었으나 권한이 없는 요청 → 403
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> write(response, ErrorCode.FORBIDDEN);
    }

    private void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getWriter(), ErrorResponse.of(errorCode));
    }
}
