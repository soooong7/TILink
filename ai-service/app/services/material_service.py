import logging
import time

from sqlalchemy.orm import Session

from app.core.database import SessionLocal
from app.models.material import Material, ProcessingStatus

logger = logging.getLogger(__name__)

# 실제 파싱·청킹·임베딩 대신 걸어 두는 더미 지연(초). 5단계에서 통째로 사라진다.
DUMMY_WORK_SECONDS = 3


def find_material(db: Session, material_id: str) -> Material | None:
    return db.get(Material, material_id)


def process_material(material_id: str) -> None:
    """
    학습자료 처리 파이프라인 (이번 단계는 상태 전이만 하는 스텁).

    백그라운드에서 실행되므로 요청 세션을 그대로 쓰지 않고 세션을 새로 연다.
    요청 세션은 응답이 나가는 순간 닫히기 때문에 그 뒤에 쓰면 터진다.

    상태를 PROCESSING 으로 먼저 커밋하는 이유는, 처리 도중에 backend 가
    상태를 조회했을 때 '분석중'이 보여야 하기 때문이다. 한 트랜잭션에서
    PROCESSING -> DONE 을 몰아서 커밋하면 중간 상태가 밖에서 보이지 않는다.
    """
    db = SessionLocal()
    try:
        material = find_material(db, material_id)
        if material is None:
            # backend 트랜잭션이 커밋되기 전에 요청이 도착하면 여기로 온다.
            # 지금은 로그만 남기고, 재시도는 다루지 않는다.
            logger.warning("처리 요청을 받았지만 학습자료를 찾을 수 없습니다: %s", material_id)
            return

        material.processing_status = ProcessingStatus.PROCESSING
        db.commit()
        logger.info("학습자료 처리 시작: %s (%s)", material_id, material.title)

        # TODO(5단계): PDF 파싱 -> 청킹 -> 임베딩 생성 -> material_chunks 저장
        time.sleep(DUMMY_WORK_SECONDS)

        material.processing_status = ProcessingStatus.DONE
        db.commit()
        logger.info("학습자료 처리 완료: %s", material_id)
    except Exception:
        # 처리에 실패해도 프로세스를 죽이지 않는다. 상태는 PROCESSING 으로 남아
        # 어디서 멈췄는지 알 수 있다. (실패 상태 관리는 이후 단계에서 다룬다.)
        db.rollback()
        logger.exception("학습자료 처리 실패: %s", material_id)
    finally:
        db.close()
