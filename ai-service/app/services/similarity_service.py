import logging

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.exceptions import ConflictError
from app.models.material_chunk import EMBEDDING_DIMENSIONS
from app.models.til import Til
from app.schemas.til import SimilarTil, SimilarTilsRequest

logger = logging.getLogger(__name__)


def find_similar(db: Session, request: SimilarTilsRequest) -> list[SimilarTil]:
    """
    코사인 유사도가 높은 순으로 사용자의 TIL 을 찾는다.

    태그 매칭과의 결합(하이브리드 랭킹)은 여기서 하지 않는다. 태그는 backend 의
    도메인 데이터이고, 두 신호를 어떤 비율로 섞을지는 도메인 정책이라 backend 가 정한다.
    여기서는 순수 벡터 유사도만 계산한다.
    """
    embedding = _resolve_embedding(db, request)

    # cosine_distance 는 pgvector 의 <=> 연산자로 나간다. 인덱스가 vector_cosine_ops
    # 라서 다른 연산자를 쓰면 HNSW 인덱스를 타지 못한다.
    distance = Til.embedding.cosine_distance(embedding).label("distance")

    stmt = (
        select(Til.id, Til.title, Til.created_at, distance)
        # 사용자 격리. 이 조건이 빠지면 남의 TIL 이 "관련 학습"으로 노출된다.
        .where(Til.user_id == request.user_id)
        .where(Til.embedding.is_not(None))
        .order_by(distance)
        .limit(request.limit)
    )

    if request.til_id is not None:
        # 기준이 된 TIL 자신은 항상 유사도 1.0 으로 1등이 되므로 제외한다.
        stmt = stmt.where(Til.id != request.til_id)
    if request.min_similarity is not None:
        # 유사도 = 1 - 거리. 임계값을 거리 조건으로 바꿔서 DB 에서 걸러낸다.
        stmt = stmt.where(distance <= 1 - request.min_similarity)

    return [
        SimilarTil(
            til_id=row.id,
            title=row.title,
            created_at=row.created_at,
            # 소수점 4자리: 화면에 0.87 처럼 보여주기에 충분하고, 부동소수 꼬리가 응답에 섞이지 않는다.
            similarity_score=round(1 - row.distance, 4),
        )
        for row in db.execute(stmt)
    ]


def _resolve_embedding(db: Session, request: SimilarTilsRequest) -> list[float]:
    """요청이 준 벡터를 쓰거나, tilId 가 왔으면 그 TIL 의 임베딩을 읽어온다."""
    if request.embedding is not None:
        if len(request.embedding) != EMBEDDING_DIMENSIONS:
            raise ValueError(f"임베딩 차원은 {EMBEDDING_DIMENSIONS} 이어야 합니다: {len(request.embedding)}")
        return request.embedding

    til = db.get(Til, request.til_id)
    if til is None or til.user_id != request.user_id:
        # 남의 TIL 인지 없는 TIL 인지 구분해서 알려주지 않는다 (존재 여부 노출 방지).
        raise LookupError(request.til_id)
    if til.embedding is None:
        raise ConflictError(f"아직 임베딩이 없는 TIL 입니다: {request.til_id}")

    return list(til.embedding)
