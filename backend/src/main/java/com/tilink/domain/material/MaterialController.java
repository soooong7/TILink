package com.tilink.domain.material;

import com.tilink.domain.material.dto.MaterialResponse;
import com.tilink.domain.material.dto.MaterialUploadRequest;
import com.tilink.global.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
}
