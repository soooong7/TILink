package com.tilink.domain.material;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 학습자료를 일정 크기로 잘라낸 청크와 그 임베딩 벡터. RAG 검색의 실제 대상이다.
 *
 * <p>FastAPI 가 PDF 를 파싱·청킹하고 임베딩을 생성해 이 테이블에 쌓는다.
 */
@Entity
@Table(name = "material_chunks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class MaterialChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false, updatable = false)
    private Material material;

    /** 자료 안에서의 청크 순서 (0부터). (material_id, chunk_index) 조합은 유일하다. */
    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    /**
     * 청크 임베딩 벡터 (text-embedding-3-small 기준 1536차원).
     *
     * <p>JPA 표준에는 벡터 타입이 없다. {@code @JdbcTypeCode(SqlTypes.VECTOR)} 를 붙이면
     * hibernate-vector 모듈이 Java 의 {@code float[]} 와 PostgreSQL 의 {@code vector} 타입을
     * 자동으로 변환해 준다.
     */
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(name = "embedding", nullable = false, columnDefinition = "vector(1536)")
    private float[] embedding;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private MaterialChunk(Material material, int chunkIndex, String content, float[] embedding) {
        this.material = material;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.embedding = embedding;
    }
}
