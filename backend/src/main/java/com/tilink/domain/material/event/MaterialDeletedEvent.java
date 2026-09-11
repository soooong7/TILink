package com.tilink.domain.material.event;

/**
 * 학습자료가 삭제된 뒤 발행되는 이벤트. 저장소에 남은 PDF 를 지우는 트리거다.
 *
 * @param fileUrl 저장소 키. 행이 이미 지워진 뒤에 처리하므로 DB 를 다시 읽을 수 없어
 *                이벤트에 값을 담아 넘긴다.
 */
public record MaterialDeletedEvent(String materialId, String fileUrl) {
}
