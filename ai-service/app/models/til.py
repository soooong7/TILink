from datetime import datetime

from pgvector.sqlalchemy import Vector
from sqlalchemy import DateTime, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base
from app.models.material_chunk import EMBEDDING_DIMENSIONS


class Til(Base):
    """
    작성된 TIL. 이 서비스는 유사도 검색을 위해 **읽기만** 한다.

    TIL 의 생성·수정·삭제는 backend(Spring Boot)의 책임이다. 여기서 쓰기까지 하면
    같은 테이블에 주인이 둘이 되어 검증 규칙이 갈라진다.
    """

    __tablename__ = "tils"

    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    user_id: Mapped[str] = mapped_column(String(36))
    material_id: Mapped[str] = mapped_column(String(36))
    title: Mapped[str] = mapped_column(String(255))
    content: Mapped[str] = mapped_column(Text)
    # 저장 직후 임베딩 생성 전일 수 있어 NULL 이 가능하다. 검색에서는 제외한다.
    embedding: Mapped[list[float] | None] = mapped_column(Vector(EMBEDDING_DIMENSIONS), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime)
    updated_at: Mapped[datetime] = mapped_column(DateTime)
