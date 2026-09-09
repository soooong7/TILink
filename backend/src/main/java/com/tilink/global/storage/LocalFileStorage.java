package com.tilink.global.storage;

import com.tilink.global.error.BusinessException;
import com.tilink.global.error.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 로컬 디스크 파일 저장소.
 *
 * <p>저장 경로는 {@code {root}/{userId}/{yyyy}/{MM}/{uuid}.pdf} 다. 사용자·연월로 디렉터리를
 * 나누는 이유는 한 디렉터리에 파일이 수만 개 쌓이면 파일시스템 조회가 급격히 느려지기 때문이다.
 * 파일명은 UUID 로 새로 짓는다 (원본 이름을 그대로 쓰면 경로 탈출·덮어쓰기 위험이 있다).
 */
@Slf4j
@Component
public class LocalFileStorage implements FileStorage {

    private static final DateTimeFormatter YEAR = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM");

    private final Path root;

    public LocalFileStorage(FileStorageProperties properties) {
        // toAbsolutePath().normalize(): 이후 경로 탈출 검사를 위해 기준 경로를 미리 확정해 둔다.
        this.root = Path.of(properties.local().root()).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile store(MultipartFile file, String userId) {
        String originalFileName = resolveOriginalFileName(file);
        String key = buildKey(userId, originalFileName);
        Path target = resolveInsideRoot(key);

        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            // 원인(디스크 가득 참, 권한 등)은 로그로만 남기고 클라이언트에는 일반 메시지를 준다.
            log.error("파일 저장 실패. key={}", key, e);
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR);
        }

        return new StoredFile(key, originalFileName, file.getSize());
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolveInsideRoot(key));
        } catch (IOException e) {
            // 삭제 실패로 업로드 롤백 같은 상위 흐름까지 막지는 않는다. 고아 파일은 로그로 추적한다.
            log.warn("파일 삭제 실패. key={}", key, e);
        }
    }

    // 저장 키. 확장자는 원본에서 가져오되 소문자로 통일한다.
    private String buildKey(String userId, String originalFileName) {
        LocalDate today = LocalDate.now();
        return "%s/%s/%s/%s%s".formatted(
                userId,
                today.format(YEAR),
                today.format(MONTH),
                UUID.randomUUID(),
                extension(originalFileName));
    }

    private String extension(String fileName) {
        String extension = StringUtils.getFilenameExtension(fileName);
        return extension == null ? "" : "." + extension.toLowerCase();
    }

    /**
     * 키를 실제 경로로 바꾸되, 결과가 반드시 root 안에 있는지 확인한다.
     * 키에 {@code ../} 가 섞여 들어와 저장 루트 바깥 파일을 건드리는 것을 막는 마지막 방어선이다.
     */
    private Path resolveInsideRoot(String key) {
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root)) {
            log.warn("저장 루트를 벗어난 경로 접근. key={}", key);
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }
        return resolved;
    }

    /**
     * 원본 파일명을 안전하게 뽑는다. 브라우저에 따라 전체 경로가 담겨 오는 경우가 있어
     * {@code StringUtils.cleanPath} 로 정리한 뒤 마지막 경로 요소만 쓴다.
     */
    private String resolveOriginalFileName(MultipartFile file) {
        String raw = file.getOriginalFilename();
        if (!StringUtils.hasText(raw)) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }
        String cleaned = StringUtils.cleanPath(raw);
        String fileName = Path.of(cleaned).getFileName().toString();
        // materials.original_file_name 이 varchar(255) 라 그보다 긴 이름은 애초에 받지 않는다.
        if (!StringUtils.hasText(fileName) || fileName.contains("..") || fileName.length() > 255) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }
        return fileName;
    }
}
