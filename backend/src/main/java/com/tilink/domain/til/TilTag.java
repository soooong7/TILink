package com.tilink.domain.til;

import com.tilink.domain.tag.Tag;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * TIL 과 태그의 N:M 연결.
 *
 * <p>{@code @ManyToMany} 를 쓰면 코드는 짧지만 중간 테이블을 직접 조회할 수 없다.
 * 이 서비스의 핵심인 "관련 학습 조회"는 til_tags 를 조인해 <b>겹치는 태그 개수</b>를 세야
 * 하므로, 중간 테이블을 별도 엔티티로 명시적으로 매핑한다.
 */
@Entity
@Table(name = "til_tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TilTag {

    @EmbeddedId
    private TilTagId id;

    /**
     * {@code @MapsId("tilId")}: 이 연관관계의 Til.id 값을 복합키의 tilId 필드에 그대로 쓰겠다는 뜻.
     * 덕분에 til_id 값을 두 군데(키와 연관관계)에 중복으로 넣지 않아도 된다.
     */
    @MapsId("tilId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "til_id", nullable = false, updatable = false)
    private Til til;

    @MapsId("tagId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false, updatable = false)
    private Tag tag;

    public TilTag(Til til, Tag tag) {
        this.id = new TilTagId(til.getId(), tag.getId());
        this.til = til;
        this.tag = tag;
    }
}
