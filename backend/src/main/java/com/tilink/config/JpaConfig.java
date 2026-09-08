package com.tilink.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 공통 설정.
 *
 * <p>{@code @EnableJpaAuditing} 을 켜면 엔티티의 {@code @CreatedDate} /
 * {@code @LastModifiedDate} 필드에 저장·수정 시각이 자동으로 채워진다.
 * (엔티티 쪽에는 {@code @EntityListeners(AuditingEntityListener.class)} 가 필요하다)
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
