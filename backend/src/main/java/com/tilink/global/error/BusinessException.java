package com.tilink.global.error;

import lombok.Getter;

// 비즈니스 규칙 위반 예외. GlobalExceptionHandler 가 공통 응답으로 변환한다.
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
