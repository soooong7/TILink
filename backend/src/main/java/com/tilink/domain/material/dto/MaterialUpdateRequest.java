package com.tilink.domain.material.dto;

import jakarta.validation.constraints.Size;

/**
 * 학습자료 수정 요청. PATCH 이므로 보내지 않은 필드(null)는 바꾸지 않는다.
 *
 * <p>파일 교체는 범위에 없다. PDF 를 바꾸면 이미 만들어 둔 청크·임베딩이 모두 무의미해져
 * 재분석이 필요한데, 그건 업로드와 사실상 같은 흐름이라 새로 업로드하는 편이 낫다.
 *
 * @param title     새 제목. 공백만 보내면 무시하고 기존 제목을 유지한다.
 * @param subjectId 새 과목 ID
 */
public record MaterialUpdateRequest(
        @Size(max = 255, message = "제목은 255자를 넘을 수 없습니다.")
        String title,

        String subjectId) {}
