package com.tilink.global.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * FastAPI(ai-service) 호출 클라이언트.
 *
 * <p>이 클래스의 메서드는 예외를 밖으로 던지지 않는다. AI 분석은 학습자료 업로드의
 * 부가 작업이고, 분석 요청이 실패했다고 해서 이미 저장된 업로드를 실패로 만들면 안 된다.
 * 요청이 실패하면 자료는 UPLOADED 상태로 남고, 로그만 남긴다.
 */
@Component
@Slf4j
public class AiServiceClient {

    private final WebClient webClient;
    private final AiServiceProperties properties;

    public AiServiceClient(WebClient aiServiceWebClient, AiServiceProperties properties) {
        this.webClient = aiServiceWebClient;
        this.properties = properties;
    }

    /**
     * 학습자료 처리(파싱·청킹·임베딩)를 요청한다.
     *
     * <p>{@code subscribe()} 로 응답을 기다리지 않고 바로 반환한다. FastAPI 도 202 만
     * 돌려주고 실제 처리는 백그라운드에서 하므로, 여기서 기다릴 이유가 없다.
     * 처리 진행 상황은 materials.processing_status 로 확인한다.
     */
    public void requestProcessing(String materialId) {
        webClient.post()
                .uri("/materials/{materialId}/process", materialId)
                .retrieve()
                .toBodilessEntity()
                .timeout(properties.timeout())
                .doOnSuccess(response ->
                        log.info("AI 분석 요청 접수됨. materialId={}, status={}", materialId, response.getStatusCode()))
                .doOnError(e ->
                        log.warn("AI 분석 요청 실패. 자료는 UPLOADED 상태로 남는다. materialId={}, cause={}",
                                materialId, e.toString()))
                // 에러를 여기서 삼켜야 구독자 쪽으로 예외가 전파되지 않는다.
                .onErrorResume(e -> Mono.empty())
                .subscribe();
    }
}
