package com.tilink.global.ai;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 분석 서비스(FastAPI) 연동 설정.
 *
 * <p>타임아웃을 호출 성격별로 나눈 이유는 걸리는 시간이 자릿수 단위로 다르기 때문이다.
 * TIL 초안은 LLM 호출이라 실측 20초 안팎(긴 교안은 더 길다)이고, 임베딩·유사도 검색은
 * 1초 안쪽이다. 하나로 묶으면 짧은 호출이 하염없이 매달리거나 긴 호출이 잘려 나간다.
 *
 * @param baseUrl      ai-service 주소
 * @param timeout      자료 처리 요청(fire-and-forget)의 접수 응답 대기 시간
 * @param draftTimeout TIL 초안 생성 대기 시간
 * @param queryTimeout 임베딩 생성·유사도 검색 대기 시간
 */
@ConfigurationProperties("ai.service")
public record AiServiceProperties(
        String baseUrl,
        Duration timeout,
        Duration draftTimeout,
        Duration queryTimeout) {
}
