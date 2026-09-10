from datetime import datetime

from sqlalchemy import DateTime, String
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


class ProcessingStatus:
    """
    materials.processing_status 에 저장되는 값.

    backend 의 com.tilink.domain.material.ProcessingStatus enum 과 문자열이
    반드시 일치해야 한다. 한쪽만 바꾸면 다른 쪽이 조용히 깨지므로 상수로 고정해 둔다.
    """

    UPLOADED = "UPLOADED"
    PROCESSING = "PROCESSING"
    DONE = "DONE"


class Material(Base):
    """
    학습자료(PDF). 이번 단계에서는 조회와 처리 상태 갱신에만 쓴다.

    Flyway 가 만든 컬럼 중 이 서비스가 실제로 사용하는 것만 매핑했다.
    (user_id / subject_id 같은 FK 는 아직 쓸 일이 없어 빼 두었다.)
    """

    __tablename__ = "materials"

    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    title: Mapped[str] = mapped_column(String(255))
    file_url: Mapped[str] = mapped_column(String(512))
    original_file_name: Mapped[str] = mapped_column(String(255))
    processing_status: Mapped[str] = mapped_column(String(20))
    uploaded_at: Mapped[datetime] = mapped_column(DateTime)
