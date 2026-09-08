package com.tilink.domain.material;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 학습자료 청크 리포지토리.
 *
 * <p>임베딩 유사도 검색(pgvector 의 {@code <=>} 연산자)은 JPQL 로 표현할 수 없어
 * 네이티브 쿼리가 필요하다. 해당 기능을 구현하는 단계에서 추가한다.
 */
public interface MaterialChunkRepository extends JpaRepository<MaterialChunk, String> {

    /** 특정 자료의 청크를 원문 순서대로 조회한다. */
    List<MaterialChunk> findByMaterialIdOrderByChunkIndexAsc(String materialId);

    /** 자료 재처리 시 기존 청크를 모두 지우기 위해 사용한다. */
    void deleteByMaterialId(String materialId);

    long countByMaterialId(String materialId);
}
