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
        // 6항목 TIL
        String todayLearned,
        List<KeyConcept> keyConcepts,
        String practice,
        // 블로그 공유용 문서
        List<String> outline,
        List<Section> sections,
        // 두 형식이 공유하는 회고
        List<String> newLearnings,
        List<String> difficulties,
        String reflection,
        List<String> suggestedTags,
        String contentMarkdown,
        String documentMarkdown,
        int usedChunkCount,
        int totalChunkCount,
        String model) {

    public record KeyConcept(String name, String description) {
    }

    /** 문서 섹션. bodyMarkdown 에는 표·코드블록이 그대로 들어온다. */
    public record Section(String heading, String bodyMarkdown) {
    }
}
