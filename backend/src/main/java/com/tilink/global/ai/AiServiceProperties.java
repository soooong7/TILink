package com.tilink.global.ai;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 분석 서비스(FastAPI) 연동 설정.
 *
 * @param baseUrl ai-service 주소
 * @param timeout 요청 타임아웃. 처리 완료를 기다리는 시간이 아니라 "요청을 접수했다"는
 *                202 응답을 기다리는 시간이므로 짧게 잡는다.
 */
@ConfigurationProperties("ai.service")
public record AiServiceProperties(String baseUrl, Duration timeout) {
}
