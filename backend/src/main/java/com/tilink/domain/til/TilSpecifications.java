package com.tilink.domain.til;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;

/**
 * TIL 목록 조회 조건.
 *
 <p>보내지 않은 필터는 {@code Specification.unrestricted()} 를 돌려준다. null 을 돌려주면
 * {@code Specification.allOf} 가 거부한다.
 *
 * <p>JPQL 에 {@code :param is null or ...} 를 쓰지 않고 Specification 으로 만든 이유가 있다.
 * PostgreSQL 은 {@code $1 is null} 처럼 비교 대상이 없는 자리에서 파라미터 타입을 추론하지
 * 못해 {@code could not determine data type} 오류를 낸다. Criteria 는 조건을 아예 SQL 에
 * 넣지 않으므로 이 문제가 생기지 않고, 필터 조합이 늘어도 쿼리를 새로 쓰지 않아도 된다.
 */
public final class TilSpecifications {

    private TilSpecifications() {
    }

    public static Specification<Til> ownedBy(String userId) {
        return (root, query, builder) -> builder.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Til> createdFrom(LocalDate from) {
        if (from == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay());
    }

    public static Specification<Til> createdUntil(LocalDate to) {
        if (to == null) {
            return Specification.unrestricted();
        }
        // "그 날짜까지 포함"이 자연스러우므로 다음 날 0시 미만으로 비교한다.
        LocalDateTime exclusiveEnd = to.plusDays(1).atStartOfDay();
        return (root, query, builder) -> builder.lessThan(root.get("createdAt"), exclusiveEnd);
    }

    public static Specification<Til> inSubject(String subjectId) {
        if (subjectId == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) ->
                builder.equal(root.get("material").get("subject").get("id"), subjectId);
    }

    /**
     * 특정 태그가 붙은 TIL.
     *
     * <p>til_tags 를 조인하지 않고 exists 서브쿼리를 쓴다. 조인하면 태그가 여러 개인 TIL 이
     * 결과에 중복으로 나와 distinct 가 추가로 필요해진다.
     */
    public static Specification<Til> taggedWith(String tagName) {
        if (tagName == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> {
            Subquery<String> subquery = query.subquery(String.class);
            var tilTag = subquery.from(TilTag.class);
            var tag = tilTag.join("tag", JoinType.INNER);

            subquery.select(builder.literal("1"))
                    .where(builder.equal(tilTag.get("til"), root), builder.equal(tag.get("name"), tagName));

            return builder.exists(subquery);
        };
    }

    /**
     * 응답에 자료 제목·과목 이름이 항상 붙으므로 함께 읽어온다.
     * 없으면 목록 조회 때 TIL 수만큼 추가 조회가 나간다(N+1).
     */
    public static Specification<Til> fetchMaterialAndSubject() {
        return (root, query, builder) -> {
            // count 쿼리에는 fetch 를 붙일 수 없다. (페이징을 쓰게 되면 여기서 걸린다)
            if (query.getResultType() != Long.class) {
                root.fetch("material", JoinType.INNER).fetch("subject", JoinType.INNER);
            }
            // 조건을 걸지 않는 Specification 이라 항상 참인 술어를 돌려준다.
            return builder.conjunction();
        };
    }
}
