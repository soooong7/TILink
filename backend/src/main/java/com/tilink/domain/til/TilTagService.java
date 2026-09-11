package com.tilink.domain.til;

import com.tilink.domain.tag.Tag;
import com.tilink.domain.tag.TagRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * TIL 의 태그 연결을 관리한다.
 *
 * <p>태그는 전역 공용 데이터(tags)라 여러 TIL 이 같은 행을 공유한다. 그래서 "없으면 만들고
 * 있으면 재사용"하는 처리가 필요한데, 이 로직이 TilService 안에 섞이면 읽기 어려워져 분리했다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TilTagService {

    private final TagRepository tagRepository;
    private final TilTagRepository tilTagRepository;

    /** TIL 의 태그를 주어진 목록으로 통째로 교체한다. */
    public void replaceTags(Til til, List<String> tagNames) {
        removeTags(til.getId());

        List<Tag> tags = findOrCreateTags(tagNames);
        tilTagRepository.saveAll(tags.stream().map(tag -> new TilTag(til, tag)).toList());
    }

    /**
     * TIL 의 태그 연결을 모두 지운다.
     *
     * <p>삭제 직후 flush 하는 이유는 두 가지다. 이어서 같은 태그를 다시 넣을 때 JPA 가 삽입을
     * 먼저 내보내면 (til_id, tag_id) 기본키가 충돌하고, TIL 자체를 지울 때는 남아 있는
     * TilTag 가 삭제된 Til 을 가리킨 채 flush 되어 예외가 난다.
     */
    public void removeTags(String tilId) {
        tilTagRepository.deleteByIdTilId(tilId);
        tilTagRepository.flush();
    }

    /** 여러 TIL 의 태그 이름을 한 번의 조회로 모아온다. (TIL id -> 태그 이름 목록) */
    @Transactional(readOnly = true)
    public Map<String, List<String>> findTagNamesByTilIds(List<String> tilIds) {
        if (tilIds.isEmpty()) {
            return Map.of();
        }

        Map<String, List<String>> result = new LinkedHashMap<>();
        for (String tilId : tilIds) {
            result.put(tilId, new ArrayList<>());
        }
        for (TilTag tilTag : tilTagRepository.findByIdTilIdIn(tilIds)) {
            result.get(tilTag.getId().getTilId()).add(tilTag.getTag().getName());
        }
        return result;
    }

    /**
     * 이름으로 태그를 찾고, 없는 것만 새로 만든다.
     *
     * <p>이름 하나씩 조회하면 태그 수만큼 쿼리가 나가므로 {@code findByNameIn} 으로 한 번에 읽는다.
     */
    private List<Tag> findOrCreateTags(List<String> tagNames) {
        Set<String> normalized = normalize(tagNames);
        if (normalized.isEmpty()) {
            return List.of();
        }

        Map<String, Tag> existing = tagRepository.findByNameIn(normalized).stream()
                .collect(LinkedHashMap::new, (map, tag) -> map.put(tag.getName(), tag), Map::putAll);

        List<Tag> newTags = normalized.stream()
                .filter(name -> !existing.containsKey(name))
                .map(name -> Tag.builder().name(name).build())
                .toList();
        tagRepository.saveAll(newTags).forEach(tag -> existing.put(tag.getName(), tag));

        return normalized.stream().map(existing::get).toList();
    }

    /**
     * 태그 이름을 다듬는다. 앞뒤 공백을 없애고 빈 값과 중복을 버린다.
     *
     * <p>LinkedHashSet 을 쓰는 이유는 사용자가 입력한 순서를 유지하기 위해서다.
     */
    private Set<String> normalize(List<String> tagNames) {
        return tagNames.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }
}
