package com.tilink.domain.material;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 학습자료 리포지토리.
 *
 * <p>응답 DTO 가 항상 과목 이름까지 함께 내려주므로, 조회 메서드에 {@code @EntityGraph} 로
 * subject 를 함께 가져오게 했다. 없으면 목록 조회 때 자료 수만큼 subjects 조회가 더 나간다(N+1).
 */
public interface MaterialRepository extends JpaRepository<Material, String> {

    /**
     * 특정 사용자의 학습자료 목록을 최신 업로드 순으로 조회한다.
     * (엔티티에 양방향 연관관계를 만들지 않는 대신 이 메서드를 쓴다)
     */
    @EntityGraph(attributePaths = "subject")
    List<Material> findByUserIdOrderByUploadedAtDesc(String userId);

    /** 특정 사용자의 자료를 과목별로 조회한다. */
    @EntityGraph(attributePaths = "subject")
    List<Material> findByUserIdAndSubjectIdOrderByUploadedAtDesc(String userId, String subjectId);

    /** 다른 사용자의 자료에 접근하는 것을 막기 위해, 소유자까지 함께 확인하며 조회한다. */
    @EntityGraph(attributePaths = "subject")
    Optional<Material> findByIdAndUserId(String id, String userId);

    /** 처리 상태별 조회. 예: PROCESSING 상태로 멈춰 있는 자료 확인 */
    List<Material> findByProcessingStatus(ProcessingStatus processingStatus);
}
