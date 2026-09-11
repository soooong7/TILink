package com.tilink.domain.til.dto;

import com.tilink.domain.til.Til;
import java.time.LocalDateTime;
import java.util.List;

/** TIL 목록 응답. 본문은 목록에서 쓰지 않으므로 싣지 않는다. */
public record TilSummaryResponse(
        String id,
        String title,
        String materialTitle,
        String subjectName,
        List<String> tags,
        boolean embeddingReady,
        LocalDateTime lastReviewedAt,
        LocalDateTime createdAt) {

    public static TilSummaryResponse from(Til til, List<String> tags) {
        return new TilSummaryResponse(
                til.getId(),
                til.getTitle(),
                til.getMaterial().getTitle(),
                til.getMaterial().getSubject().getName(),
                tags,
                til.getEmbedding() != null,
                til.getLastReviewedAt(),
                til.getCreatedAt());
    }
}
