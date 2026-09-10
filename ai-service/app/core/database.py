from collections.abc import Generator

from sqlalchemy import create_engine
from sqlalchemy.orm import DeclarativeBase, Session, sessionmaker

from app.core.config import get_settings

engine = create_engine(
    get_settings().db_url,
    # 커넥션이 오래 놀다가 DB 쪽에서 끊긴 경우를 걸러낸다.
    pool_pre_ping=True,
)

SessionLocal = sessionmaker(bind=engine, autoflush=False, expire_on_commit=False)


class Base(DeclarativeBase):
    """
    모든 모델의 부모 클래스.

    주의: 이 서비스는 Base.metadata.create_all() 을 절대 호출하지 않는다.
    스키마의 주인은 backend 의 Flyway 이고, 여기 모델은 이미 만들어져 있는
    테이블을 읽고 쓰기 위한 매핑일 뿐이다.
    """


def get_db() -> Generator[Session, None, None]:
    # FastAPI 의존성으로 쓰는 세션. 요청이 끝나면 반드시 닫는다.
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
