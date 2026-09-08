package com.tilink.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 교육생 계정.
 *
 * <p>비밀번호 암호화·로그인 처리는 2단계(인증)에서 다룬다. 여기서는 컬럼만 정의한다.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User {

    /** PK. Hibernate 가 UUID 문자열을 생성해 채운다. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email;

    /** 암호화된 비밀번호가 저장된다. 평문을 그대로 넣지 않는다. */
    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private User(String email, String password, String name) {
        this.email = email;
        this.password = password;
        this.name = name;
    }
}
