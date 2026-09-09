package com.tilink.global.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 업로드된 파일의 저장소.
 *
 * <p>지금은 로컬 디스크({@link LocalFileStorage})만 구현되어 있지만, 나중에 S3 로 옮길 때
 * 이 인터페이스를 구현한 클래스만 새로 만들면 되도록 서비스 계층은 구현체를 모르게 둔다.
 * 그래서 반환값도 절대경로가 아니라 저장소 종류와 무관한 <b>키</b>다.
 */
public interface FileStorage {

    /**
     * 파일을 저장하고 저장 키를 돌려준다.
     *
     * @param file   업로드된 파일
     * @param userId 저장 위치를 사용자별로 나누기 위한 소유자 ID
     * @return 저장 결과(키·원본 파일명·크기)
     */
    StoredFile store(MultipartFile file, String userId);

    /** 저장 키에 해당하는 파일을 지운다. 이미 없으면 아무 일도 하지 않는다. */
    void delete(String key);
}
