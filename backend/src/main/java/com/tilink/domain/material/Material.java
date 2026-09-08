package com.tilink.domain.material;

import com.tilink.domain.subject.Subject;
import com.tilink.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 업로드된 학습자료(PDF).
 *
 * <p>연관관계는 모두 단방향 {@code @ManyToOne(LAZY)} 로만 둔다. 즉 Material -> User 는
 * 있지만 User -> List&lt;Material&gt; 은 만들지 않는다. 목록이 필요하면
 * {@code MaterialRepository.findByUserId(...)} 로 조회한다. 양방향 연관관계는
 * N+1 조회 문제와 JSON 직렬화 무한 순환의 주요 원인이라 처음부터 만들지 않는 편이 안전하다.
 */
@Entity
@Table(name = "materials")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    /** LAZY: 실제로 user 를 꺼내 쓸 때만 users 테이블을 조회한다. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    /** 업로드된 PDF 파일 경로 */
    @Column(name = "file_url", length = 512, nullable = false)
    private String fileUrl;

    /**
     * EnumType.STRING: DB에 0,1,2 같은 숫자가 아니라 'UPLOADED' 문자열 그대로 저장한다.
     * ORDINAL(숫자)로 저장하면 나중에 enum 순서를 바꿨을 때 기존 데이터의 의미가 뒤바뀐다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", length = 20, nullable = false)
    private ProcessingStatus processingStatus;

    @CreatedDate
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @Builder
    private Material(User user, Subject subject, String title, String fileUrl) {
        this.user = user;
        this.subject = subject;
        this.title = title;
        this.fileUrl = fileUrl;
        // 업로드 직후 상태는 항상 UPLOADED 로 시작한다.
        this.processingStatus = ProcessingStatus.UPLOADED;
    }

    /** AI 분석 처리 상태를 변경한다. (setter 대신 의미가 드러나는 메서드로 노출) */
    public void changeProcessingStatus(ProcessingStatus status) {
        this.processingStatus = status;
    }
}
