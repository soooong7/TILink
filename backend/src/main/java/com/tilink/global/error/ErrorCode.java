package com.tilink.global.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 서비스 전역에서 쓰는 에러 코드.
 *
 * <p>HTTP 상태와 클라이언트에 노출할 메시지를 한 곳에 모아둔다. 컨트롤러/서비스는
 * 상태 코드를 직접 다루지 않고 이 enum 만 던진다.
 */
@Getter
public enum ErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    // 이메일이 없는 경우와 비밀번호가 틀린 경우를 구분하지 않는다.
    // 구분해서 응답하면 어떤 이메일이 가입되어 있는지 외부에서 알아낼 수 있다.
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
