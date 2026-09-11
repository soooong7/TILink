package com.tilink.global.ai.dto;

/** ai-service 의 임베딩 생성 요청/응답. */
public final class AiEmbedding {

    private AiEmbedding() {
    }

    public record Request(String text) {
    }

    // embedding 을 float[] 로 받는 이유: 엔티티의 벡터 컬럼 타입과 같아 변환이 필요 없다.
    public record Response(float[] embedding, int dimensions, String model) {
    }
}
