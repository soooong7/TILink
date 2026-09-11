package com.tilink.domain.til.dto;

import com.tilink.global.ai.dto.AiTilDraft;
import java.util.List;

/**
 * TIL 초안 응답. 아직 저장되지 않은 값이며, 사용자가 화면에서 고친 뒤 저장 API 로 보낸다.
 *
 * <p>ai-service 응답을 그대로 노출하지 않고 한 번 옮겨 담는다. 외부 서비스의 스키마가
 * 바뀌었을 때 우리 API 계약이 따라 흔들리지 않게 하기 위해서다.
 */
public record TilDraftResponse(
        String materialId,
        String title,
        String todayLearned,
        List<KeyConcept> keyConcepts,
        String practice,
        List<String> newLearnings,
        List<String> difficulties,
        String reflection,
        List<String> suggestedTags,
        String contentMarkdown,
        int usedChunkCount,
        int totalChunkCount) {

    public record KeyConcept(String name, String description) {
    }

    public static TilDraftResponse from(AiTilDraft draft) {
        return new TilDraftResponse(
                draft.materialId(),
                draft.title(),
                draft.todayLearned(),
                draft.keyConcepts().stream().map(c -> new KeyConcept(c.name(), c.description())).toList(),
                draft.practice(),
                draft.newLearnings(),
                draft.difficulties(),
                draft.reflection(),
                draft.suggestedTags(),
                draft.contentMarkdown(),
                draft.usedChunkCount(),
                draft.totalChunkCount());
    }
}
