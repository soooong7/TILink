package com.tilink.domain.til;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * TIL-태그 연결 리포지토리.
 *
 * <p>PK 가 복합키이므로 두 번째 제네릭 타입은 {@link TilTagId} 다.
 * 메서드 이름의 {@code IdTilId} 는 "id 필드 안의 tilId 필드"를 뜻한다.
 */
public interface TilTagRepository extends JpaRepository<TilTag, TilTagId> {

    /** 특정 TIL 에 붙은 태그 연결 목록 */
    List<TilTag> findByIdTilId(String tilId);

    /** 특정 태그가 붙은 TIL 연결 목록 (태그 기반 관련 학습 조회의 기초) */
    List<TilTag> findByIdTagId(String tagId);

    /** TIL 태그를 통째로 교체할 때, 기존 연결을 먼저 지우기 위해 사용한다. */
    void deleteByIdTilId(String tilId);
}
