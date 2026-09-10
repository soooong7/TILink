package com.tilink.domain.material.event;

import com.tilink.global.ai.AiServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 업로드 완료 후 AI 분석을 요청한다.
 *
 * <p>{@code AFTER_COMMIT} 인 이유: FastAPI 는 같은 PostgreSQL 에서 materials 행을 직접
 * 읽는다. 커밋 전에 요청을 보내면 FastAPI 쪽에서는 아직 그 행이 보이지 않아 404 가 난다.
 *
 * <p>이 리스너에서 예외가 나도 이미 커밋된 업로드 트랜잭션은 롤백되지 않는다.
 * 그래도 호출 자체를 {@link AiServiceClient} 안에서 비동기·예외 없이 처리해서,
 * 응답 지연이 업로드 API 응답 시간에 섞이지 않게 한다.
 */
@Component
@RequiredArgsConstructor
public class MaterialUploadedEventListener {

    private final AiServiceClient aiServiceClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void requestAiProcessing(MaterialUploadedEvent event) {
        aiServiceClient.requestProcessing(event.materialId());
    }
}
