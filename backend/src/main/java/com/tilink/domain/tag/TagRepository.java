package com.tilink.domain.tag;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 태그 리포지토리. */
public interface TagRepository extends JpaRepository<Tag, String> {

    Optional<Tag> findByName(String name);

    /**
     * 이름 목록으로 태그를 한 번에 조회한다.
     * AI가 추천한 태그들 중 이미 존재하는 것을 골라낼 때 사용한다. (이름 하나씩 N번 조회하는 것을 방지)
     */
    List<Tag> findByNameIn(Collection<String> names);
}
