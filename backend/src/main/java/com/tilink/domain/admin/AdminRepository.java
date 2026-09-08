package com.tilink.domain.admin;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 서비스 관리자 리포지토리. */
public interface AdminRepository extends JpaRepository<Admin, String> {

    Optional<Admin> findByEmail(String email);

    boolean existsByEmail(String email);
}
