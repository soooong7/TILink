package com.tilink.domain.material.event;

// 학습자료가 저장된 뒤 발행되는 이벤트. AI 분석 요청의 트리거다.
public record MaterialUploadedEvent(String materialId) {
}
