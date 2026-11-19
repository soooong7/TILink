package com.tilink.domain.material.dto;

import org.springframework.core.io.Resource;

/**
 * 학습자료 원본 파일 다운로드 결과.
 *
 * @param resource         파일 내용
 * @param originalFileName 다운로드 시 보여줄 파일명. 저장 키(UUID)가 아니라 사용자가 올린 이름이다.
 */
public record MaterialFileResponse(Resource resource, String originalFileName) {}
