package com.tilink.domain.tag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기술/주제 태그. 예: "Spring AI", "Docker"
 *
 * <p>관련 학습 조회에서 벡터 유사도와 함께 쓰이는 보조 매칭 신호다.
 * ERD에 created_at 이 없으므로 Auditing 을 적용하지 않는다.
 */
@Entity
@Table(name = "tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;

    @Builder
    private Tag(String name) {
        this.name = name;
    }
}
