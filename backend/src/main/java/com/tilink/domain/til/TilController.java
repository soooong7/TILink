package com.tilink.domain.til;

import com.tilink.domain.til.dto.RelatedTilsResponse;
import com.tilink.domain.til.dto.TilCreateRequest;
import com.tilink.domain.til.dto.TilResponse;
import com.tilink.domain.til.dto.TilReviewResponse;
import com.tilink.domain.til.dto.TilSummaryResponse;
import com.tilink.domain.til.dto.TilUpdateRequest;
import com.tilink.global.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * TIL API.
 *
 * <p>사용자 ID 는 요청 파라미터나 바디로 받지 않고 인증 토큰에서만 꺼낸다.
 * 그래야 다른 사람의 TIL 을 조회하도록 요청을 조작할 수 없다.
 */
@RestController
@RequestMapping("/api/tils")
@RequiredArgsConstructor
public class TilController {

    private final TilService tilService;

    /** TIL 저장. */
    @PostMapping
    public ResponseEntity<TilResponse> create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody TilCreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(tilService.create(principal.id(), request));
    }

    /**
     * 내 TIL 목록.
     *
     * @param from 작성일 시작(포함), @param to 작성일 끝(포함)
     */
    @GetMapping
    public List<TilSummaryResponse> findMine(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String subjectId,
            @RequestParam(required = false) String tag) {

        return tilService.findMine(principal.id(), from, to, subjectId, tag);
    }

    /**
     * 복습이 필요한 TIL 목록.
     *
     * <p>{@code /{tilId}} 보다 먼저 선언한다. 경로가 겹치지는 않지만, 고정 경로를 위에 두면
     * 읽는 사람이 "needs-review 가 TIL ID 로 해석되지 않나?" 하고 의심할 일이 없다.
     */
    @GetMapping("/needs-review")
    public List<TilReviewResponse> findNeedsReview(@AuthenticationPrincipal AuthenticatedUser principal) {
        return tilService.findNeedsReview(principal.id());
    }

    /** TIL 상세. 조회 시점에 최종 열람 일시가 갱신된다. */
    @GetMapping("/{tilId}")
    public TilResponse findOne(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String tilId) {

        return tilService.findOne(principal.id(), tilId);
    }

    /** TIL 수정. 보내지 않은 필드는 바뀌지 않는다. */
    @PatchMapping("/{tilId}")
    public TilResponse update(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String tilId,
            @Valid @RequestBody TilUpdateRequest request) {

        return tilService.update(principal.id(), tilId, request);
    }

    /** TIL 삭제. */
    @DeleteMapping("/{tilId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String tilId) {

        tilService.delete(principal.id(), tilId);
        return ResponseEntity.noContent().build();
    }

    /** 관련 학습 조회. 벡터 유사도와 태그 일치를 결합한 순서로 돌려준다. */
    @GetMapping("/{tilId}/related")
    public RelatedTilsResponse findRelated(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String tilId,
            @RequestParam(required = false) Integer limit) {

        return tilService.findRelated(principal.id(), tilId, limit);
    }
}
