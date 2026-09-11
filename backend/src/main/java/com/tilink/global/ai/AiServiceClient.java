package com.tilink.global.ai;

import com.tilink.global.ai.dto.AiEmbedding;
import com.tilink.global.ai.dto.AiSimilarTils;
import com.tilink.global.ai.dto.AiTilDraft;
import com.tilink.global.error.BusinessException;
import com.tilink.global.error.ErrorCode;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * ai-service(FastAPI) 호출 클라이언트.
 *
 * <p>메서드마다 실패 정책이 다르다.
 * <ul>
 *   <li>{@link #requestProcessing} 은 예외를 던지지 않는다. 업로드는 이미 성공했고,
 *       분석 요청 실패가 업로드를 되돌리면 안 된다.</li>
 *   <li>나머지는 예외를 던진다. 호출한 서비스가 상황에 맞게 처리한다
 *       (초안 생성은 사용자에게 전파, 임베딩 실패는 삼키고 저장 진행).</li>
 * </ul>
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
     * <p>{@code subscribe()} 로 응답을 기다리지 않고 바로 반환한다. ai-service 도 202 만
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

    /** TIL 초안을 생성한다. LLM 호출이라 오래 걸리므로 draftTimeout 을 쓴다. */
    public AiTilDraft generateTilDraft(String materialId) {
        return call("TIL 초안 생성", properties.draftTimeout(), () -> webClient.post()
                .uri("/materials/{materialId}/til-draft", materialId)
                .retrieve()
                .bodyToMono(AiTilDraft.class));
    }

    /** 텍스트의 임베딩(1536차원)을 생성한다. */
    public float[] createEmbedding(String text) {
        AiEmbedding.Response response = call("임베딩 생성", properties.queryTimeout(), () -> webClient.post()
                .uri("/embeddings")
                .bodyValue(new AiEmbedding.Request(text))
                .retrieve()
                .bodyToMono(AiEmbedding.Response.class));

        return response.embedding();
    }

    /** 벡터 유사도가 높은 TIL 을 찾는다. 태그를 결합한 최종 순위는 호출한 쪽에서 정한다. */
    public AiSimilarTils.Response findSimilarTils(AiSimilarTils.Request request) {
        return call("유사 TIL 검색", properties.queryTimeout(), () -> webClient.post()
                .uri("/tils/similar")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiSimilarTils.Response.class));
    }

    /**
     * 동기 호출 공통 처리.
     *
     * <p>재시도는 하지 않는다. ai-service 가 이미 OpenAI 호출을 재시도하고 있고, LLM 호출은
     * 느리고 비싸서 여기서 또 재시도하면 사용자 대기 시간만 배로 늘어난다.
     *
     * <p>{@code block()} 은 요청을 처리 중인 서블릿 스레드에서 호출된다. 이 애플리케이션은
     * Web MVC 라 이벤트 루프 스레드를 막지 않으므로 안전하다.
     */
    private <T> T call(String operation, Duration timeout, Supplier<Mono<T>> request) {
        try {
            T response = request.get().timeout(timeout).block();
            if (response == null) {
                throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
            }
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            // 타임아웃과 그 외 오류를 구분해서 알린다. 504 를 받은 클라이언트는
            // "다시 시도하면 될 수도 있다"고 판단할 수 있다.
            boolean timedOut = hasCause(e, TimeoutException.class);
            log.error("{} 실패. timedOut={}, cause={}", operation, timedOut, e.toString());
            throw new BusinessException(timedOut ? ErrorCode.AI_SERVICE_TIMEOUT : ErrorCode.AI_SERVICE_ERROR);
        }
    }

    private boolean hasCause(Throwable error, Class<? extends Throwable> type) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
        }
        return false;
    }
}
