package com.tilink.domain.material.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 학습자료 업로드 요청 중 파일을 제외한 값들.
 *
 * <p>multipart 요청이라 파일({@code MultipartFile})은 컨트롤러에서 따로 받고,
 * 나머지 필드만 이 DTO 로 묶어 검증한다.
 *
 * @param subjectId 소속 과목 ID
 * @param title     학습자료 제목. 비어 있으면 업로드한 파일명에서 확장자를 뗀 값을 쓴다.
 */
public record MaterialUploadRequest(
        @NotBlank(message = "과목 ID는 필수입니다.")
        String subjectId,

        @Size(max = 255, message = "제목은 255자를 넘을 수 없습니다.")
        String title) {}
