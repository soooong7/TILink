package com.tilink.domain.material;

import com.tilink.domain.material.dto.MaterialFileResponse;
import com.tilink.domain.material.dto.MaterialResponse;
import com.tilink.domain.material.dto.MaterialUpdateRequest;
import com.tilink.domain.material.dto.MaterialUploadRequest;
import com.tilink.domain.til.TilService;
import com.tilink.domain.til.dto.TilDraftResponse;
import com.tilink.global.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 학습자료 API.
 *
 * <p>사용자 ID 는 요청 파라미터나 바디로 받지 않고 인증 토큰에서만 꺼낸다.
 * 그래야 다른 사람의 자료를 조회하도록 요청을 조작할 수 없다.
 */
@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;
    private final TilService tilService;

    /**
     * 학습자료 PDF 업로드.
     *
     * <p>{@code @ModelAttribute} 로 받으면 multipart 폼 필드(subjectId, title)가 DTO 로 묶이고,
     * 파일만 {@code @RequestPart} 로 따로 받는다.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MaterialResponse> upload(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid MaterialUploadRequest request,
            @RequestPart("file") MultipartFile file) {

        MaterialResponse response = materialService.upload(principal.id(), request, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** 내 학습자료 목록. subjectId 를 주면 해당 과목만 조회한다. */
    @GetMapping
    public List<MaterialResponse> findMine(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) String subjectId) {

        return materialService.findMine(principal.id(), subjectId);
    }

    /** 내 학습자료 상세. */
    @GetMapping("/{materialId}")
    public MaterialResponse findOne(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String materialId) {

        return materialService.findOne(principal.id(), materialId);
    }

    /**
     * 학습자료 원본 PDF 다운로드.
     *
     * <p>파일명은 RFC 6266 의 {@code filename*=UTF-8''...} 형식으로 내려간다. 한글 파일명을
     * 그대로 {@code filename=} 에 넣으면 브라우저마다 깨지는 방식이 다르다.
     */
    @GetMapping("/{materialId}/file")
    public ResponseEntity<Resource> downloadFile(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String materialId) {

        MaterialFileResponse file = materialService.loadFile(principal.id(), materialId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.originalFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.resource());
    }

    /** 학습자료 수정. 보내지 않은 필드는 바뀌지 않는다. */
    @PatchMapping("/{materialId}")
    public MaterialResponse update(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String materialId,
            @Valid @RequestBody MaterialUpdateRequest request) {

        return materialService.update(principal.id(), materialId, request);
    }

    /** 학습자료 삭제. 이 자료로 만든 TIL 과 청크, 저장된 PDF 도 함께 사라진다. */
    @DeleteMapping("/{materialId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String materialId) {

        materialService.delete(principal.id(), materialId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 학습자료로부터 TIL 초안을 생성한다. 저장하지 않고 그대로 돌려준다.
     *
     * <p>경로가 /api/materials 아래인 이유는 "이 자료로부터 만든다"는 관계를 드러내기 위해서다.
     * 저장된 TIL 을 다루는 API 는 /api/tils 에 있다.
     */
    @PostMapping("/{materialId}/til-draft")
    public TilDraftResponse createTilDraft(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String materialId) {

        return tilService.createDraft(principal.id(), materialId);
    }
}
