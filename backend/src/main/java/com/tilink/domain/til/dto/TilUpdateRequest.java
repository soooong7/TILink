package com.tilink.domain.til.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * TIL 수정 요청. PATCH 이므로 보내지 않은 필드(null)는 바꾸지 않는다.
 *
 * <p>태그는 "보낸 목록으로 통째로 교체"다. 빈 배열을 보내면 태그를 모두 떼겠다는 뜻이고,
 * null 이면 태그를 건드리지 않는다.
 */
public record TilUpdateRequest(
        @Size(max = 255) String title,
        String content,
        List<@Size(max = 100) String> tags) {
}
