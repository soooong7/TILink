package com.tilink.global.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// 파일 저장소 설정 프로퍼티(storage.*)를 바인딩해 빈으로 등록한다.
@Configuration
@EnableConfigurationProperties(FileStorageProperties.class)
public class StorageConfig {
}
