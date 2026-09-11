package com.tilink.domain.til.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 관련 학습 조회 응답. (기획서 9장의 응답 형식)
 *
 * @param similarityScore 벡터 코사인 유사도 원본. 태그를 결합한 하이브리드 점수는 정렬에만
 *                        쓰고 노출하지 않는다. 가중치는 언제든 조정될 내부 값이라 API 계약에
 *                        넣지 않는 편이 낫다.
 * @param matchedTags     현재 TIL 과 겹친 태그. "왜 관련 있다고 나왔는지"를 보여주는 근거다.
 */
public record RelatedTilsResponse(List<RelatedTil> relatedTils) {

    public record RelatedTil(
            String id,
            String title,
            LocalDate date,
            double similarityScore,
            List<String> matchedTags) {
    }
}
