package com.tilink.domain.subject;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 과목/기술 카테고리 리포지토리. */
public interface SubjectRepository extends JpaRepository<Subject, String> {

    Optional<Subject> findByName(String name);

    boolean existsByName(String name);

    List<Subject> findAllByOrderByNameAsc();
}
