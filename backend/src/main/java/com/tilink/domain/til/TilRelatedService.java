package com.tilink.domain.til;

import com.tilink.domain.til.dto.RelatedTilsResponse;
import com.tilink.global.ai.AiServiceClient;
import com.tilink.global.ai.dto.AiSimilarTils;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관련 학습 조회 — 벡터 유사도와 태그 일치를 결합한 하이브리드 랭킹.
 *
 * <p>벡터 검색은 ai-service(pgvector)가, 태그 결합은 여기가 담당한다. 태그는 backend 의
 * 도메인 데이터이고 두 신호를 어떤 비율로 섞을지는 도메인 정책이기 때문이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TilRelatedService {

    private final AiServiceClient aiServiceClient;
    private final TilTagService tilTagService;
    private final TilRelatedProperties properties;

    public List<RelatedTilsResponse.RelatedTil> findRelated(String userId, Til til, int limit) {
        List<AiSimilarTils.SimilarTil> candidates = fetchCandidates(userId, til.getId(), limit);
        if (candidates.isEmpty()) {
            return List.of();
        }

        Set<String> baseTags = new HashSet<>(tagNamesOf(til.getId()));
        Map<String, List<String>> candidateTags =
                tilTagService.findTagNamesByTilIds(candidates.stream().map(AiSimilarTils.SimilarTil::tilId).toList());

        return candidates.stream()
                .map(candidate -> score(candidate, baseTags, candidateTags.getOrDefault(candidate.tilId(), List.of())))
                .sorted(Comparator.comparingDouble(Scored::hybridScore).reversed())
                .limit(limit)
                .map(Scored::toResponse)
                .toList();
    }

    /**
     * 벡터 유사도 상위 후보를 넉넉히 받아온다.
     *
     * <p>최종 개수만큼만 받으면 태그 점수로 재정렬해도 순위가 바뀔 여지가 없다.
     * 태그가 겹치는 TIL 이 벡터 순위 6~10위에 있을 수 있다.
     */
    private List<AiSimilarTils.SimilarTil> fetchCandidates(String userId, String tilId, int limit) {
        int candidateLimit = Math.min(limit * properties.candidateMultiplier(), 50);
        AiSimilarTils.Request request =
                new AiSimilarTils.Request(userId, tilId, candidateLimit, properties.minSimilarity());

        return aiServiceClient.findSimilarTils(request).results();
    }

    private Scored score(AiSimilarTils.SimilarTil candidate, Set<String> baseTags, List<String> tags) {
        List<String> matched = tags.stream().filter(baseTags::contains).toList();

        return new Scored(
                candidate,
                matched,
                properties.vectorWeight() * candidate.similarityScore()
                        + properties.tagWeight() * jaccard(baseTags, tags, matched.size()));
    }

    /**
     * 태그 자카드 유사도 = 겹친 태그 수 / 두 TIL 태그의 합집합 크기.
     *
     * <p>단순히 "겹친 개수"를 쓰면 태그를 많이 단 TIL 이 항상 유리해진다.
     * 합집합으로 나누면 태그 개수에 중립적인 0~1 값이 된다.
     */
    private double jaccard(Set<String> baseTags, List<String> tags, int matchedCount) {
        int union = baseTags.size() + tags.size() - matchedCount;
        return union == 0 ? 0 : (double) matchedCount / union;
    }

    private List<String> tagNamesOf(String tilId) {
        return tilTagService.findTagNamesByTilIds(List.of(tilId)).getOrDefault(tilId, List.of());
    }

    private record Scored(AiSimilarTils.SimilarTil candidate, List<String> matchedTags, double hybridScore) {

        RelatedTilsResponse.RelatedTil toResponse() {
            return new RelatedTilsResponse.RelatedTil(
                    candidate.tilId(),
                    candidate.title(),
                    candidate.createdAt().toLocalDate(),
                    candidate.similarityScore(),
                    matchedTags);
        }
    }
}
