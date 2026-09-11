package com.tilink.domain.til;

import com.tilink.domain.material.Material;
import com.tilink.domain.user.User;
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
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 생성·저장된 TIL.
 *
 * <p>본문 임베딩({@code embedding})은 관련 학습 조회에, 최종 열람 일시
 * ({@code lastReviewedAt})는 복습 알림 판단에 쓰인다. 둘 다 저장 직후에는 값이 없을 수
 * 있으므로 nullable 이다.
 */
@Entity
@Table(name = "tils")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Til {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    /** 이 TIL 의 기반이 된 학습자료 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false, updatable = false)
    private Material material;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    /** TIL 본문. 기획서의 6항목 템플릿을 담은 마크다운이며, 임베딩도 이 값으로 만든다. */
    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    /**
     * 블로그 공유용 긴 문서(목차 + 주제별 섹션). content 와 쓰임이 달라 따로 둔다.
     * 문서를 만들지 않고 저장한 TIL 이 있을 수 있어 null 을 허용한다.
     */
    @Column(name = "document_markdown", columnDefinition = "text")
    private String documentMarkdown;

    /**
     * TIL 본문 임베딩 (1536차원). 아직 생성되지 않았을 수 있어 null 을 허용한다.
     * 매핑 방식은 {@code MaterialChunk.embedding} 과 동일하다.
     */
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(name = "embedding", columnDefinition = "vector(1536)")
    private float[] embedding;

    /** 최종 열람 일시. 한 번도 열람하지 않았다면 null 이며, 복습 알림의 기준값이다. */
    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private Til(User user, Material material, String title, String content, String documentMarkdown,
            float[] embedding) {
        this.user = user;
        this.material = material;
        this.title = title;
        this.content = content;
        this.documentMarkdown = documentMarkdown;
        this.embedding = embedding;
    }

    /** 사용자가 TIL 내용을 수정했을 때 호출한다. */
    public void updateContent(String title, String content, String documentMarkdown) {
        this.title = title;
        this.content = content;
        this.documentMarkdown = documentMarkdown;
    }

    /** 본문이 바뀌어 임베딩을 다시 생성했을 때 호출한다. */
    public void updateEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    /** TIL 을 열람했을 때 최종 열람 일시를 갱신한다. (복습 알림 기준값 갱신) */
    public void markReviewed(LocalDateTime reviewedAt) {
        this.lastReviewedAt = reviewedAt;
    }
}
