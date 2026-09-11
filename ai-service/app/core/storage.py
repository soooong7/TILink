import logging
from pathlib import Path

from app.core.config import get_settings
from app.core.exceptions import ProcessingError

logger = logging.getLogger(__name__)


def resolve(file_key: str) -> Path:
    """
    materials.file_url 에 저장된 상대 키를 실제 파일 경로로 바꾼다.

    DB 에는 절대경로가 아니라 저장 루트 기준 상대 키가 들어 있어서, 루트를 붙여야
    파일을 열 수 있다. 지금은 backend 와 같은 로컬 디렉터리를 공유하는 방식이고,
    S3 로 옮기면 이 모듈만 교체하면 된다.

    키가 DB 에서 온 값이라도 경로 탈출 검사는 한다. 잘못된 키 하나로 저장 루트
    바깥 파일을 읽는 일이 없어야 한다 (backend 의 LocalFileStorage 와 같은 방어선).
    """
    root = Path(get_settings().storage_root).resolve()
    target = (root / file_key).resolve()

    if not target.is_relative_to(root):
        raise ProcessingError(f"저장 루트를 벗어난 파일 경로입니다: {file_key}")
    if not target.is_file():
        # 대부분 파일이 없는 게 아니라 STORAGE_ROOT 가 backend 와 다른 곳을 가리키는 경우다.
        # (backend 의 상대 경로 기본값은 JVM 작업 디렉터리 기준이라 실행 위치에 따라 달라진다)
        raise ProcessingError(
            f"파일을 찾을 수 없습니다: {target} "
            f"(STORAGE_ROOT={root} 가 backend 의 저장 루트와 같은지 확인하세요)"
        )

    return target


def log_storage_root() -> None:
    # 기동 로그에 남겨 두면 경로가 어긋났을 때 바로 눈에 띈다.
    logger.info("학습자료 저장 루트: %s", Path(get_settings().storage_root).resolve())
