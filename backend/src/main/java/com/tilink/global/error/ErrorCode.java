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
    SUBJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "과목을 찾을 수 없습니다."),
    // 남의 자료를 조회한 경우도 403 이 아니라 이 404 로 응답한다.
    // 403 으로 구분해 주면 "그 ID 의 자료가 존재한다"는 사실이 외부에 노출된다.
    MATERIAL_NOT_FOUND(HttpStatus.NOT_FOUND, "학습자료를 찾을 수 없습니다."),
    INVALID_FILE(HttpStatus.BAD_REQUEST, "PDF 파일만 업로드할 수 있습니다."),
    // 413. Spring 7 에서 PAYLOAD_TOO_LARGE 는 CONTENT_TOO_LARGE 로 이름이 바뀌었다(같은 코드).
    FILE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "업로드할 수 있는 파일 크기를 초과했습니다."),
    FILE_STORAGE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "파일을 저장하지 못했습니다."),
    TIL_NOT_FOUND(HttpStatus.NOT_FOUND, "TIL 을 찾을 수 없습니다."),
    // 본문 임베딩이 아직 없는 TIL 로 관련 학습을 조회한 경우.
    // AI 서비스 장애로 임베딩 없이 저장된 TIL 이 있을 수 있어 상태로 구분해 알린다.
    TIL_EMBEDDING_NOT_READY(HttpStatus.CONFLICT, "아직 관련 학습을 조회할 준비가 되지 않은 TIL 입니다."),
    // 502/504. AI 서비스는 우리 서버 입장에서 외부 의존성이므로 5xx 로 알린다.
    AI_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "AI 서비스 호출에 실패했습니다."),
    AI_SERVICE_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "AI 서비스 응답이 지연되고 있습니다. 잠시 후 다시 시도해 주세요."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
