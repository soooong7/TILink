package com.tilink.domain.til;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * TIL 리포지토리.
 *
 * <p>임베딩 유사도 기반 "관련 학습 조회"는 pgvector 네이티브 쿼리가 필요하므로
 * 해당 기능을 구현하는 단계에서 추가한다.
 */
public interface TilRepository extends JpaRepository<Til, String> {

    List<Til> findByUserIdOrderByCreatedAtDesc(String userId);

    /** 다른 사용자의 TIL 에 접근하는 것을 막기 위해, 작성자까지 함께 확인하며 조회한다. */
    Optional<Til> findByIdAndUserId(String id, String userId);

    /** 특정 학습자료로부터 생성된 TIL 목록 */
    List<Til> findByMaterialId(String materialId);

    /**
     * 복습이 필요한 TIL 조회.
     *
     * <p>기준 시각보다 오래 전에 열람했거나(lastReviewedAt &lt; threshold),
     * 한 번도 열람하지 않은(lastReviewedAt IS NULL) TIL 을 함께 찾는다.
     * SQL 에서 NULL 은 어떤 비교 연산에도 참이 되지 않으므로 조건을 따로 써 줘야 한다.
     */
    List<Til> findByUserIdAndLastReviewedAtBeforeOrUserIdAndLastReviewedAtIsNull(
            String userIdForBefore, LocalDateTime threshold, String userIdForNull);
}
