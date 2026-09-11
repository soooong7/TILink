package com.tilink.domain.til;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 관련 학습 조회 설정.
 *
 * @param vectorWeight         하이브리드 점수에서 벡터 유사도의 가중치
 * @param tagWeight            태그 자카드 유사도의 가중치
 * @param minSimilarity        이보다 낮은 벡터 유사도는 후보에서 제외한다
 * @param candidateMultiplier  최종 개수의 몇 배를 후보로 받을지
 * @param defaultLimit         limit 을 지정하지 않았을 때의 개수
 */
@ConfigurationProperties("til.related")
public record TilRelatedProperties(
        double vectorWeight,
        double tagWeight,
        double minSimilarity,
        int candidateMultiplier,
        int defaultLimit) {
}
