package com.tilink.global.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 파일 저장소 설정.
 *
 * @param local 로컬 디스크 저장소 설정
 */
@ConfigurationProperties(prefix = "storage")
public record FileStorageProperties(Local local) {

    /**
     * @param root 업로드 파일을 저장할 루트 디렉터리. 상대 경로면 애플리케이션 실행 디렉터리 기준이다.
     */
    public record Local(String root) {}
}
