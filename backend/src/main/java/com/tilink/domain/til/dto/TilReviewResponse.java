package com.tilink.domain.til.dto;

import com.tilink.domain.til.Til;
import java.time.LocalDateTime;

/**
 * 복습이 필요한 TIL 응답.
 *
 * @param daysSinceReview 마지막으로 열람한 지 며칠 지났는지. 한 번도 열지 않았다면 작성일 기준이다.
 * @param neverReviewed   한 번도 열람하지 않은 TIL 인지
 */
public record TilReviewResponse(
        String id,
        String title,
        String subjectName,
        long daysSinceReview,
        boolean neverReviewed,
        LocalDateTime lastReviewedAt,
        LocalDateTime createdAt) {

    public static TilReviewResponse from(Til til, long daysSinceReview) {
        return new TilReviewResponse(
                til.getId(),
                til.getTitle(),
                til.getMaterial().getSubject().getName(),
                daysSinceReview,
                til.getLastReviewedAt() == null,
                til.getLastReviewedAt(),
                til.getCreatedAt());
    }
}
