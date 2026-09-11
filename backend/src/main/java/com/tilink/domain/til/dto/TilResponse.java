package com.tilink.domain.til.dto;

import com.tilink.domain.til.Til;
import java.time.LocalDateTime;
import java.util.List;

/**
 * TIL 상세 응답.
 *
 * @param embeddingReady 본문 임베딩이 준비됐는지. AI 서비스 장애로 임베딩 없이 저장된 TIL 은
 *                       관련 학습 조회를 할 수 없으므로, 화면에서 구분해 보여줄 수 있게 내려준다.
 */
public record TilResponse(
        String id,
        String title,
        String content,
        MaterialSummary material,
        List<String> tags,
        boolean embeddingReady,
        LocalDateTime lastReviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public record MaterialSummary(String id, String title, String subjectId, String subjectName) {
    }

    public static TilResponse from(Til til, List<String> tags) {
        return new TilResponse(
                til.getId(),
                til.getTitle(),
                til.getContent(),
                new MaterialSummary(
                        til.getMaterial().getId(),
                        til.getMaterial().getTitle(),
                        til.getMaterial().getSubject().getId(),
                        til.getMaterial().getSubject().getName()),
                tags,
                til.getEmbedding() != null,
                til.getLastReviewedAt(),
                til.getCreatedAt(),
                til.getUpdatedAt());
    }
}
