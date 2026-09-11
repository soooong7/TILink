package com.tilink.domain.til;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * TIL 리포지토리.
 *
 * <p>응답에 항상 자료 제목과 과목 이름이 붙으므로 조회 메서드에 {@code @EntityGraph} 로
 * material·subject 를 함께 가져온다. 없으면 목록 조회 때 TIL 수만큼 추가 조회가 나간다(N+1).
 *
 * <p>임베딩 유사도 검색은 여기 없다. ai-service 가 pgvector 검색을 담당하고,
 * backend 는 그 결과에 태그 점수를 결합하는 역할만 한다.
 */
public interface TilRepository extends JpaRepository<Til, String>, JpaSpecificationExecutor<Til> {

    /** 다른 사용자의 TIL 에 접근하는 것을 막기 위해, 작성자까지 함께 확인하며 조회한다. */
    @EntityGraph(attributePaths = {"material", "material.subject"})
    Optional<Til> findByIdAndUserId(String id, String userId);

    @EntityGraph(attributePaths = {"material", "material.subject"})
    List<Til> findByIdInAndUserId(Collection<String> ids, String userId);

    /** 특정 학습자료로부터 생성된 TIL 목록 */
    List<Til> findByMaterialId(String materialId);

    /**
     * 복습이 필요한 TIL.
     *
     * <p>기준값은 {@code coalesce(lastReviewedAt, createdAt)} 이다. 한 번도 열람하지 않은
     * TIL 을 무조건 복습 대상으로 넣으면 방금 작성한 TIL 까지 목록에 뜬다. 열람 기록이
     * 없으면 작성 시점부터 경과일을 센다는 뜻이다.
     */
    @EntityGraph(attributePaths = {"material", "material.subject"})
    @Query("""
            select t from Til t
            where t.user.id = :userId
              and coalesce(t.lastReviewedAt, t.createdAt) < :threshold
            order by coalesce(t.lastReviewedAt, t.createdAt) asc
            """)
    List<Til> findNeedsReview(@Param("userId") String userId, @Param("threshold") LocalDateTime threshold);
}
