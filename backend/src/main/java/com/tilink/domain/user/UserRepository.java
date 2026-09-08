package com.tilink.domain.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 교육생 리포지토리.
 *
 * <p>JpaRepository&lt;엔티티, PK타입&gt; 를 상속하면 save/findById/findAll/delete 등이 자동 제공된다.
 * 아래처럼 규칙에 맞는 이름의 메서드를 선언하면 Spring Data JPA 가 쿼리를 자동 생성한다.
 */
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
