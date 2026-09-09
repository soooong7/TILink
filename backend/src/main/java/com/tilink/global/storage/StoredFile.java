package com.tilink.global.storage;

/**
 * 파일 저장 결과.
 *
 * @param key              저장소 안에서 파일을 가리키는 키. 저장 루트에 대한 상대 경로 형태이며
 *                         ({@code {userId}/2026/09/{uuid}.pdf}) DB 의 {@code materials.file_url} 에 그대로 들어간다.
 *                         절대경로를 저장하지 않는 이유는, 저장 루트가 바뀌거나 S3 로 옮겨도
 *                         이미 저장된 데이터를 고치지 않아도 되게 하기 위해서다.
 * @param originalFileName 사용자가 업로드한 원본 파일명 (화면 표시용)
 * @param size             파일 크기(바이트)
 */
public record StoredFile(String key, String originalFileName, long size) {}
