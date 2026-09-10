package com.tilink.domain.material;

import com.tilink.domain.material.dto.MaterialResponse;
import com.tilink.domain.material.event.MaterialUploadedEvent;
import com.tilink.domain.material.dto.MaterialUploadRequest;
import com.tilink.domain.subject.Subject;
import com.tilink.domain.subject.SubjectRepository;
import com.tilink.domain.user.User;
import com.tilink.domain.user.UserRepository;
import com.tilink.global.error.BusinessException;
import com.tilink.global.error.ErrorCode;
import com.tilink.global.storage.FileStorage;
import com.tilink.global.storage.StoredFile;
import java.util.List;
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
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
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
            return MaterialResponse.from(material);
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

        return materials.stream().map(MaterialResponse::from).toList();
    }

    /** 내 학습자료 상세. 남의 자료를 요청하면 존재 여부를 알리지 않도록 404 로 응답한다. */
    public MaterialResponse findOne(String userId, String materialId) {
        return materialRepository.findByIdAndUserId(materialId, userId)
                .map(MaterialResponse::from)
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
