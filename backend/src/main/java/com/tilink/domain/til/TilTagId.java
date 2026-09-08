package com.tilink.domain.til;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * til_tags 테이블의 복합 기본키 (til_id, tag_id).
 *
 * <p>JPA 에서 두 컬럼을 묶어 PK 로 쓰려면 이렇게 별도의 {@code @Embeddable} 클래스가 필요하다.
 * 이 클래스는 반드시 아래 3가지를 만족해야 한다.
 * <ul>
 *   <li>{@code Serializable} 구현</li>
 *   <li>기본 생성자 보유</li>
 *   <li>{@code equals()} / {@code hashCode()} 재정의 — JPA 가 두 키가 같은지 비교하는 기준</li>
 * </ul>
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TilTagId implements Serializable {

    @Column(name = "til_id", length = 36, nullable = false)
    private String tilId;

    @Column(name = "tag_id", length = 36, nullable = false)
    private String tagId;

    public TilTagId(String tilId, String tagId) {
        this.tilId = tilId;
        this.tagId = tagId;
    }
}
