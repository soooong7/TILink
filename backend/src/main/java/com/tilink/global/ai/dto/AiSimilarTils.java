package com.tilink.global.ai.dto;

import java.time.LocalDateTime;
import java.util.List;

/** ai-service 의 유사 TIL 검색 요청/응답. */
public final class AiSimilarTils {

    private AiSimilarTils() {
    }

    /**
     * @param userId 사용자 격리용. ai-service 에는 인증이 없으므로 이 값이 빠지면
     *               남의 TIL 이 결과에 섞인다. 반드시 토큰에서 꺼낸 값을 넣는다.
     * @param tilId 기준 TIL. 자기 자신은 결과에서 제외된다.
     * @param limit 후보 개수. 태그 점수로 재정렬할 것이므로 최종 개수보다 넉넉히 받는다.
     */
    public record Request(String userId, String tilId, int limit, Double minSimilarity) {
    }

    public record Response(List<SimilarTil> results, int count) {
    }

    public record SimilarTil(String tilId, String title, LocalDateTime createdAt, double similarityScore) {
    }
}
