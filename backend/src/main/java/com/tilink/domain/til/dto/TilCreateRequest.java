package com.tilink.domain.til.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * TIL 저장 요청.
 *
 * @param tags 태그 이름 목록. 초안의 suggestedTags 를 사용자가 고친 결과가 들어온다.
 *             없는 태그는 저장 시점에 만들어진다.
 */
public record TilCreateRequest(
        @NotBlank String materialId,
        @NotBlank @Size(max = 255) String title,
        @NotBlank String content,
        List<@Size(max = 100) String> tags) {

    public List<String> tagsOrEmpty() {
        return tags == null ? List.of() : tags;
    }
}
