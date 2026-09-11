from datetime import datetime

from pgvector.sqlalchemy import Vector
from sqlalchemy import DateTime, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base

# material_chunks.embedding 의 차원. DB 컬럼이 vector(1536) 이고 HNSW 인덱스도
# 같은 차원으로 만들어져 있어서, 모델을 바꾸려면 마이그레이션이 함께 필요하다.
EMBEDDING_DIMENSIONS = 1536


class MaterialChunk(Base):
    """
    학습자료를 청킹한 조각과 그 임베딩. 6단계의 유사도 검색 대상이다.

    (material_id, chunk_index) 에 UNIQUE 제약이 걸려 있어서 재처리할 때는
    기존 청크를 지우고 다시 넣어야 한다.
    """

    __tablename__ = "material_chunks"

    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    material_id: Mapped[str] = mapped_column(String(36))
    chunk_index: Mapped[int] = mapped_column(Integer)
    content: Mapped[str] = mapped_column(Text)
    embedding: Mapped[list[float]] = mapped_column(Vector(EMBEDDING_DIMENSIONS))
    created_at: Mapped[datetime] = mapped_column(DateTime)
