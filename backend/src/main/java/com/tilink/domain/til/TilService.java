package com.tilink.domain.til;

import com.tilink.domain.material.Material;
import com.tilink.domain.material.MaterialRepository;
import com.tilink.domain.til.dto.RelatedTilsResponse;
import com.tilink.domain.til.dto.TilCreateRequest;
import com.tilink.domain.til.dto.TilDraftResponse;
import com.tilink.domain.til.dto.TilResponse;
import com.tilink.domain.til.dto.TilReviewResponse;
import com.tilink.domain.til.dto.TilSummaryResponse;
import com.tilink.domain.til.dto.TilUpdateRequest;
import com.tilink.domain.user.User;
import com.tilink.domain.user.UserRepository;
import com.tilink.global.ai.AiServiceClient;
import com.tilink.global.error.BusinessException;
import com.tilink.global.error.ErrorCode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * TIL 관리 서비스.
 *
 * <p>조회는 모두 "로그인한 사용자 본인의 TIL"로 한정한다. TIL ID 만으로 조회하는 메서드를
 * 두지 않고 항상 userId 를 함께 넘기는 이유는, 소유자 검사를 빠뜨릴 여지를 없애기 위해서다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TilService {

    /** 복습 알림 기준. 이 기간 이상 열람하지 않은 TIL 을 복습 대상으로 본다. */
    private static final int REVIEW_THRESHOLD_DAYS = 7;

    private final TilRepository tilRepository;
    private final MaterialRepository materialRepository;
    private final UserRepository userRepository;
    private final TilTagService tilTagService;
    private final TilRelatedService tilRelatedService;
    private final TilRelatedProperties relatedProperties;
    private final AiServiceClient aiServiceClient;

    /**
     * TIL 초안을 생성한다. 저장하지 않고 그대로 돌려준다.
     *
     * <p>사용자가 화면에서 고친 뒤 저장 API 로 보내는 흐름이라, 여기서 저장하면
     * 쓰지 않을 TIL 이 쌓인다.
     */
    public TilDraftResponse createDraft(String userId, String materialId) {
        // 남의 자료로 초안을 만들 수 없도록 소유자까지 확인한다.
        materialRepository.findByIdAndUserId(materialId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATERIAL_NOT_FOUND));

        return TilDraftResponse.from(aiServiceClient.generateTilDraft(materialId));
    }

    /** TIL 을 저장한다. 본문 임베딩과 태그 연결도 함께 만든다. */
    @Transactional
    public TilResponse create(String userId, TilCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Material material = materialRepository.findByIdAndUserId(request.materialId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATERIAL_NOT_FOUND));

        Til til = tilRepository.save(Til.builder()
                .user(user)
                .material(material)
                .title(request.title())
                .content(request.content())
                .embedding(createEmbeddingOrNull(request.content()))
                .build());

        tilTagService.replaceTags(til, request.tagsOrEmpty());
        return TilResponse.from(til, tagNamesOf(til.getId()));
    }

    /** 내 TIL 목록. 기간·과목·태그 필터는 모두 선택이다. */
    public List<TilSummaryResponse> findMine(String userId, LocalDate from, LocalDate to, String subjectId, String tagName) {
        // allOf 는 null 인 Specification 을 건너뛴다. 덕분에 보내지 않은 필터는
        // 조건 자체가 SQL 에 들어가지 않는다.
        Specification<Til> spec = Specification.allOf(
                TilSpecifications.ownedBy(userId),
                TilSpecifications.createdFrom(from),
                TilSpecifications.createdUntil(to),
                TilSpecifications.inSubject(StringUtils.hasText(subjectId) ? subjectId : null),
                TilSpecifications.taggedWith(StringUtils.hasText(tagName) ? tagName.trim() : null),
                TilSpecifications.fetchMaterialAndSubject());

        List<Til> tils = tilRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));

        Map<String, List<String>> tags = tilTagService.findTagNamesByTilIds(tils.stream().map(Til::getId).toList());
        return tils.stream()
                .map(til -> TilSummaryResponse.from(til, tags.getOrDefault(til.getId(), List.of())))
                .toList();
    }

    /**
     * TIL 상세. 조회 시점에 최종 열람 일시를 갱신한다.
     *
     * <p>조회 API 지만 쓰기 트랜잭션인 이유가 여기 있다. 복습 알림을 별도 배치 없이
     * 구현하기 위해, 열람 사실을 조회 시점에 기록한다(기획서 5장).
     */
    @Transactional
    public TilResponse findOne(String userId, String tilId) {
        Til til = getOwnedTil(userId, tilId);
        til.markReviewed(LocalDateTime.now());
        return TilResponse.from(til, tagNamesOf(tilId));
    }

    /**
     * TIL 수정.
     *
     * <p>본문이 실제로 바뀐 경우에만 임베딩을 다시 만든다. 제목만 고쳤는데 임베딩을
     * 새로 호출하면 비용과 지연만 늘어난다.
     */
    @Transactional
    public TilResponse update(String userId, String tilId, TilUpdateRequest request) {
        Til til = getOwnedTil(userId, tilId);

        String title = StringUtils.hasText(request.title()) ? request.title() : til.getTitle();
        String content = StringUtils.hasText(request.content()) ? request.content() : til.getContent();
        boolean contentChanged = !content.equals(til.getContent());

        til.updateContent(title, content);
        // 임베딩이 없는 상태(저장 당시 AI 서비스 장애)라면 본문이 그대로여도 다시 시도한다.
        if (contentChanged || til.getEmbedding() == null) {
            til.updateEmbedding(createEmbeddingOrNull(content));
        }
        if (request.tags() != null) {
            tilTagService.replaceTags(til, request.tags());
        }

        return TilResponse.from(til, tagNamesOf(tilId));
    }

    /**
     * TIL 삭제.
     *
     * <p>DB 의 til_tags FK 가 ON DELETE CASCADE 이지만, 연결을 먼저 지운다. JPA 는 DB 의
     * cascade 를 모르기 때문에, 영속성 컨텍스트에 남아 있는 TilTag 가 삭제된 Til 을 가리킨
     * 채로 flush 되면서 TransientPropertyValueException 이 난다.
     */
    @Transactional
    public void delete(String userId, String tilId) {
        Til til = getOwnedTil(userId, tilId);
        tilTagService.removeTags(tilId);
        tilRepository.delete(til);
    }

    /** 관련 학습 조회. 벡터 유사도와 태그 일치를 결합해 정렬한다. */
    public RelatedTilsResponse findRelated(String userId, String tilId, Integer limit) {
        Til til = getOwnedTil(userId, tilId);
        if (til.getEmbedding() == null) {
            // 임베딩이 없으면 유사도를 계산할 수 없다. 빈 목록을 주면 "관련 자료가 없다"는
            // 뜻으로 읽히므로, 준비되지 않았다는 사실을 상태로 구분해 알린다.
            throw new BusinessException(ErrorCode.TIL_EMBEDDING_NOT_READY);
        }

        int size = limit == null ? relatedProperties.defaultLimit() : limit;
        return new RelatedTilsResponse(tilRelatedService.findRelated(userId, til, size));
    }

    /**
     * 복습이 필요한 TIL 목록.
     *
     * <p>별도 배치·스케줄러 없이 조회 시점에 경과일을 계산한다(기획서 5장).
     */
    public List<TilReviewResponse> findNeedsReview(String userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusDays(REVIEW_THRESHOLD_DAYS);

        return tilRepository.findNeedsReview(userId, threshold).stream()
                .map(til -> TilReviewResponse.from(til, daysSince(til, now)))
                .toList();
    }

    private long daysSince(Til til, LocalDateTime now) {
        // 열람 기록이 없으면 작성 시점부터 센다.
        LocalDateTime base = til.getLastReviewedAt() == null ? til.getCreatedAt() : til.getLastReviewedAt();
        return Duration.between(base, now).toDays();
    }

    private Til getOwnedTil(String userId, String tilId) {
        // 남의 TIL 을 요청하면 존재 여부를 알리지 않도록 404 로 응답한다.
        return tilRepository.findByIdAndUserId(tilId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TIL_NOT_FOUND));
    }

    private List<String> tagNamesOf(String tilId) {
        return tilTagService.findTagNamesByTilIds(List.of(tilId)).getOrDefault(tilId, List.of());
    }

    /**
     * 본문 임베딩을 만든다. 실패하면 null 을 돌려주고 저장은 계속한다.
     *
     * <p>임베딩은 관련 학습 조회에만 쓰이는 부가 값이다. AI 서비스 장애 때문에 사용자가
     * 작성한 TIL 자체를 잃는 것이 훨씬 나쁘다. 임베딩이 빠진 TIL 은 수정 시 다시 시도한다.
     */
    private float[] createEmbeddingOrNull(String content) {
        try {
            return aiServiceClient.createEmbedding(content);
        } catch (BusinessException e) {
            log.warn("TIL 본문 임베딩 생성 실패. 임베딩 없이 저장한다. code={}", e.getErrorCode());
            return null;
        }
    }
}
