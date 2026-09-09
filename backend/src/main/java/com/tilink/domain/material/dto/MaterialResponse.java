package com.tilink.domain.material.dto;

import com.tilink.domain.material.Material;
import com.tilink.domain.material.ProcessingStatus;
import com.tilink.domain.subject.dto.SubjectResponse;
import java.time.LocalDateTime;

/**
 * 학습자료 응답.
 *
 * @param fileUrl 저장소 키. 파일 자체를 내려주는 API 는 아직 없고, 이후 다운로드 API 가
 *                이 키로 파일을 찾게 된다.
 */
public record MaterialResponse(
        String id,
        String title,
        String originalFileName,
        String fileUrl,
        SubjectResponse subject,
        ProcessingStatus processingStatus,
        LocalDateTime uploadedAt) {

    public static MaterialResponse from(Material material) {
        return new MaterialResponse(
                material.getId(),
                material.getTitle(),
                material.getOriginalFileName(),
                material.getFileUrl(),
                SubjectResponse.from(material.getSubject()),
                material.getProcessingStatus(),
                material.getUploadedAt());
    }
}
