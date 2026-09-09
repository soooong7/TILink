package com.tilink.global.error;

/**
 * 모든 에러 응답의 공통 형태.
 *
 * @param code    {@link ErrorCode} 이름 (클라이언트 분기용)
 * @param message 사용자에게 보여줄 메시지
 */
public record ErrorResponse(String code, String message) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.name(), message);
    }
}
