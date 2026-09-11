package com.tilink.domain.material.event;

import com.tilink.global.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 자료 삭제가 커밋된 뒤 저장소의 PDF 를 지운다.
 *
 * <p>{@code AFTER_COMMIT} 인 이유는 업로드와 정반대다. 파일 삭제는 되돌릴 수 없으므로,
 * 트랜잭션 안에서 지웠다가 롤백되면 DB 에는 자료가 남았는데 파일만 사라진 상태가 된다.
 * 커밋이 확정된 뒤에 지우면 최악의 경우에도 "지워지지 않은 고아 파일"만 남는다.
 */
@Component
@RequiredArgsConstructor
public class MaterialDeletedEventListener {

    private final FileStorage fileStorage;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deleteStoredFile(MaterialDeletedEvent event) {
        fileStorage.delete(event.fileUrl());
    }
}
