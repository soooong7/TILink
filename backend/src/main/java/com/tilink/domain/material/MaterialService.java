package com.tilink.domain.material;

import com.tilink.domain.material.dto.MaterialFileResponse;
import com.tilink.domain.material.dto.MaterialResponse;
import com.tilink.domain.material.dto.MaterialUpdateRequest;
import com.tilink.domain.material.dto.MaterialUploadRequest;
import com.tilink.domain.material.event.MaterialDeletedEvent;
import com.tilink.domain.material.event.MaterialUploadedEvent;
import com.tilink.domain.subject.Subject;
import com.tilink.domain.subject.SubjectRepository;
import com.tilink.domain.til.TilService;
import com.tilink.domain.user.User;
import com.tilink.domain.user.UserRepository;
import com.tilink.global.error.BusinessException;
import com.tilink.global.error.ErrorCode;
import com.tilink.global.storage.FileStorage;
import com.tilink.global.storage.StoredFile;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 학습자료 관리 서비스.
 *
 * <p>조회는 모두 "로그인한 사용자 본인의 자료"로 한정한다. 자료 ID 만으로 조회하는 메서드를
 * 두지 않고 항상 userId 를 함께 넘기는 이유는, 소유자 검사를 빠뜨릴 여지를 없애기 위해서다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaterialService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String PDF_EXTENSION = "pdf";

    private final MaterialRepository materialRepository;
    private final MaterialChunkRepository materialChunkRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final TilService tilService;
    private final FileStorage fileStorage;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * PDF 를 저장소에 저장하고 학습자료로 등록한다.
     *
     * <p>파일 저장은 DB 트랜잭션이 롤백돼도 되돌아가지 않는다. 그래서 DB 저장이 실패하면
     * 방금 쓴 파일을 직접 지워 고아 파일이 남지 않게 한다.
     *
     * <p>저장이 끝나면 AI 분석 요청 이벤트를 발행한다. 여기서 FastAPI 를 직접 호출하지 않는
     * 이유는 커밋 전에 요청이 나가면 안 되기 때문이다 (MaterialUploadedEventListener 참고).
     */
    @Transactional
    public MaterialResponse upload(String userId, MaterialUploadRequest request, MultipartFile file) {
        validatePdf(file);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Subject subject = subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBJECT_NOT_FOUND));

        StoredFile storedFile = fileStorage.store(file, userId);
        try {
            Material material = materialRepository.save(Material.builder()
                    .user(user)
                    .subject(subject)
                    .title(resolveTitle(request.title(), storedFile.originalFileName()))
                    .fileUrl(storedFile.key())
                    .originalFileName(storedFile.originalFileName())
                    .build());
            eventPublisher.publishEvent(new MaterialUploadedEvent(material.getId()));
            // 방금 만든 자료라 TIL 은 아직 하나도 없다.
            return MaterialResponse.from(material, 0);
        } catch (RuntimeException e) {
            fileStorage.delete(storedFile.key());
            throw e;
        }
    }

    /** 내 학습자료 목록. subjectId 가 있으면 해당 과목으로만 좁힌다. */
    public List<MaterialResponse> findMine(String userId, String subjectId) {
        List<Material> materials = StringUtils.hasText(subjectId)
                ? materialRepository.findByUserIdAndSubjectIdOrderByUploadedAtDesc(userId, subjectId)
                : materialRepository.findByUserIdOrderByUploadedAtDesc(userId);

        // 자료마다 count 쿼리를 날리지 않도록 TIL 개수는 한 번에 모아온다.
        Map<String, Long> tilCounts = tilService.countByMaterials(materials.stream().map(Material::getId).toList());
        return materials.stream()
                .map(material -> MaterialResponse.from(material, tilCounts.getOrDefault(material.getId(), 0L)))
                .toList();
    }

    /** 내 학습자료 상세. 남의 자료를 요청하면 존재 여부를 알리지 않도록 404 로 응답한다. */
    public MaterialResponse findOne(String userId, String materialId) {
        Material material = getOwnedMaterial(userId, materialId);
        return MaterialResponse.from(material, tilService.countByMaterial(materialId));
    }

    /** 내 학습자료의 원본 PDF. 소유자 검사는 상세 조회와 같은 규칙(남의 자료는 404)을 따른다. */
    public MaterialFileResponse loadFile(String userId, String materialId) {
        Material material = getOwnedMaterial(userId, materialId);
        return new MaterialFileResponse(fileStorage.load(material.getFileUrl()), material.getOriginalFileName());
    }

    /**
     * 학습자료 수정. 보내지 않은 필드는 바뀌지 않는다.
     *
     * <p>제목과 과목만 바꾼다. 둘 다 분류상의 값이라 이미 만들어 둔 청크·임베딩에는 영향이 없고,
     * 따라서 재분석도 필요 없다.
     */
    @Transactional
    public MaterialResponse update(String userId, String materialId, MaterialUpdateRequest request) {
        Material material = getOwnedMaterial(userId, materialId);

        if (StringUtils.hasText(request.title())) {
            material.changeTitle(request.title().trim());
        }
        if (StringUtils.hasText(request.subjectId()) && !request.subjectId().equals(material.getSubject().getId())) {
            Subject subject = subjectRepository.findById(request.subjectId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.SUBJECT_NOT_FOUND));
            material.changeSubject(subject);
        }

        return MaterialResponse.from(material, tilService.countByMaterial(materialId));
    }

    /**
     * 학습자료 삭제. 이 자료로 만든 TIL 과 청크도 함께 사라진다.
     *
     * <p>DB FK 는 모두 ON DELETE CASCADE 지만 JPA 는 그 사실을 모르기 때문에, 자식(TIL·청크)부터
     * 순서대로 지운다. 남아 있는 자식 엔티티가 지워진 부모를 가리킨 채 flush 되면 예외가 난다.
     *
     * <p>PDF 파일은 여기서 지우지 않고 커밋 이후에 지운다
     * ({@link com.tilink.domain.material.event.MaterialDeletedEventListener} 참고).
     */
    @Transactional
    public void delete(String userId, String materialId) {
        Material material = getOwnedMaterial(userId, materialId);
        String fileUrl = material.getFileUrl();

        tilService.deleteAllByMaterial(materialId);
        materialChunkRepository.deleteByMaterialId(materialId);
        materialRepository.delete(material);

        eventPublisher.publishEvent(new MaterialDeletedEvent(materialId, fileUrl));
    }

    /** 소유자까지 확인해 자료를 가져온다. 남의 자료를 요청하면 존재 여부를 알리지 않도록 404 다. */
    private Material getOwnedMaterial(String userId, String materialId) {
        return materialRepository.findByIdAndUserId(materialId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATERIAL_NOT_FOUND));
    }

    /**
     * 업로드된 파일이 PDF 인지 확인한다.
     *
     * <p>확장자와 content-type 을 모두 본다. 둘 다 클라이언트가 보내는 값이라 완전한 보증은
     * 아니지만, 5단계에서 PDF 파서가 엉뚱한 파일을 받는 상황을 대부분 걸러 준다.
     */
    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (extension == null || !PDF_EXTENSION.equalsIgnoreCase(extension)) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith(PDF_CONTENT_TYPE)) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }
    }

    // 제목을 안 보냈으면 파일명에서 확장자를 뗀 값을 제목으로 쓴다.
    private String resolveTitle(String title, String originalFileName) {
        if (StringUtils.hasText(title)) {
            return title.trim();
        }
        return StringUtils.stripFilenameExtension(originalFileName);
    }
}
