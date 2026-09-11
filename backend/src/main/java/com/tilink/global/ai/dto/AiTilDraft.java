package com.tilink.global.ai.dto;

import java.util.List;

/**
 * ai-service 의 TIL 초안 응답.
 *
 * <p>필드 이름이 ai-service 의 JSON(camelCase)과 1:1 로 대응한다. 이 record 는 외부
 * 응답을 받는 전용 타입이고, 우리 API 응답으로는 도메인 DTO 로 변환해서 내보낸다.
 */
public record AiTilDraft(
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
        int totalChunkCount,
        String model) {

    public record KeyConcept(String name, String description) {
    }
}
