package com.tilink.domain.material;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 학습자료 리포지토리. */
public interface MaterialRepository extends JpaRepository<Material, String> {

    /**
     * 특정 사용자의 학습자료 목록을 최신 업로드 순으로 조회한다.
     * (엔티티에 양방향 연관관계를 만들지 않는 대신 이 메서드를 쓴다)
     */
    List<Material> findByUserIdOrderByUploadedAtDesc(String userId);

    /** 특정 사용자의 자료를 과목별로 조회한다. */
    List<Material> findByUserIdAndSubjectIdOrderByUploadedAtDesc(String userId, String subjectId);

    /** 다른 사용자의 자료에 접근하는 것을 막기 위해, 소유자까지 함께 확인하며 조회한다. */
    Optional<Material> findByIdAndUserId(String id, String userId);

    /** 처리 상태별 조회. 예: PROCESSING 상태로 멈춰 있는 자료 확인 */
    List<Material> findByProcessingStatus(ProcessingStatus processingStatus);
}
