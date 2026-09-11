import logging
import uuid
from datetime import datetime

from sqlalchemy import delete
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.core.database import SessionLocal
from app.core.exceptions import ProcessingError
from app.core import storage
from app.models.material import Material, ProcessingStatus
from app.models.material_chunk import MaterialChunk
from app.services import chunker, embedding_service, pdf_parser

logger = logging.getLogger(__name__)


def find_material(db: Session, material_id: str) -> Material | None:
    return db.get(Material, material_id)


def process_material(material_id: str) -> None:
    """
    학습자료 처리 파이프라인: PDF 파싱 -> 청킹 -> 임베딩 -> material_chunks 저장.

    백그라운드에서 실행되므로 요청 세션을 그대로 쓰지 않고 세션을 새로 연다.
    요청 세션은 응답이 나가는 순간 닫히기 때문에 그 뒤에 쓰면 터진다.

    상태를 PROCESSING 으로 먼저 커밋하는 이유는, 처리 도중에 backend 가 상태를
    조회했을 때 '분석중'이 보여야 하기 때문이다. 한 트랜잭션에서 몰아서 커밋하면
    중간 상태가 밖에서 보이지 않는다.
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

        try:
            chunk_count = _run_pipeline(db, material)
        except Exception as e:
            _mark_failed(db, material_id, e)
            return

        material.processing_status = ProcessingStatus.DONE
        db.commit()
        logger.info("학습자료 처리 완료: %s (청크 %d개)", material_id, chunk_count)
    finally:
        db.close()


def _run_pipeline(db: Session, material: Material) -> int:
    settings = get_settings()

    path = storage.resolve(material.file_url)
    text = pdf_parser.extract_text(path)
    chunks = chunker.chunk_text(
        text,
        chunk_size=settings.chunk_size,
        overlap=settings.chunk_overlap,
        min_chunk_size=settings.min_chunk_size,
    )

    if not chunks:
        # 스캔 이미지로만 된 PDF 가 대표적이다. 조용히 DONE 이 되면 6단계에서
        # "검색은 되는데 아무것도 안 나오는" 자료가 된다. 실패로 끝내는 편이 낫다.
        raise ProcessingError("PDF 에서 추출된 텍스트가 없습니다 (스캔 이미지 PDF 일 수 있습니다)")

    logger.info("청크 %d개 생성: %s", len(chunks), material.id)

    # 임베딩을 전부 만든 뒤에 저장한다. 만들면서 저장하면 중간에 실패했을 때
    # 자료의 일부만 검색되는 상태가 남는다.
    embeddings = embedding_service.create_embeddings(chunks)
    _replace_chunks(db, material.id, chunks, embeddings)
    return len(chunks)


def _replace_chunks(db: Session, material_id: str, chunks: list[str], embeddings: list[list[float]]) -> None:
    """
    기존 청크를 지우고 새 청크를 한 트랜잭션에 저장한다.

    지우고 시작하는 이유는 두 가지다. (material_id, chunk_index) 에 UNIQUE 제약이
    있어서 재처리 시 그대로 넣으면 충돌하고, 이전 실패로 남은 잔여물도 정리해야 한다.
    """
    now = datetime.now()

    db.execute(delete(MaterialChunk).where(MaterialChunk.material_id == material_id))
    db.add_all([
        MaterialChunk(
            id=str(uuid.uuid4()),
            material_id=material_id,
            chunk_index=index,
            content=content,
            embedding=embedding,
            created_at=now,
        )
        for index, (content, embedding) in enumerate(zip(chunks, embeddings, strict=True))
    ])
    db.commit()


def _mark_failed(db: Session, material_id: str, error: Exception) -> None:
    """
    처리 실패를 기록한다. 여기서 예외를 다시 던지지 않는 이유는, 실패 처리 중에
    또 실패해서 상태가 PROCESSING 으로 굳는 상황을 막기 위해서다.
    """
    logger.error("학습자료 처리 실패: %s - %s", material_id, error, exc_info=True)
    db.rollback()

    try:
        # rollback 으로 세션의 객체가 만료되므로 다시 읽어온다.
        material = find_material(db, material_id)
        if material is not None:
            material.processing_status = ProcessingStatus.FAILED
            db.commit()
    except Exception:
        logger.exception("실패 상태를 기록하지 못했습니다: %s", material_id)
        db.rollback()
