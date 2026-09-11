import logging
import re
from pathlib import Path

from pypdf import PdfReader

from app.core.exceptions import ProcessingError

logger = logging.getLogger(__name__)


def extract_text(path: Path) -> str:
    """
    PDF 에서 텍스트를 추출해 하나의 문자열로 잇는다.

    페이지를 청크 단위로 쓰지 않는 이유: 교안은 슬라이드형이라 페이지당 텍스트가
    제목 + 불릿 몇 줄뿐인 경우가 많다. 페이지를 그대로 청크로 만들면 임베딩이
    지나치게 빈약해진다. 전부 이어 붙인 뒤 chunker 가 의미 단위로 다시 자른다.
    """
    try:
        reader = PdfReader(path)
        pages = [page.extract_text() or "" for page in reader.pages]
    except Exception as e:
        # pypdf 는 손상된 파일·암호화된 파일에서 다양한 예외를 던진다. 원인은 로그에 남기고
        # 호출부에는 처리 실패라는 사실만 전달한다.
        raise ProcessingError(f"PDF 를 읽을 수 없습니다: {e}") from e

    logger.info("PDF 페이지 %d개에서 텍스트를 추출했습니다: %s", len(pages), path.name)
    return normalize("\n\n".join(pages))


def normalize(text: str) -> str:
    """
    추출된 텍스트를 다듬는다.

    PDF 추출물에는 줄 끝 공백, 빈 줄 뭉치, 페이지 구분 문자가 섞여 있다. 그대로
    두면 청크 길이의 상당 부분을 공백이 차지하고, 임베딩 품질에도 도움이 안 된다.
    """
    text = text.replace("\r\n", "\n").replace("\r", "\n").replace("\x0c", "\n")
    # 줄 끝 공백 제거
    text = re.sub(r"[ \t]+\n", "\n", text)
    # 빈 줄이 3개 이상 이어지면 문단 구분(2개)으로 줄인다.
    text = re.sub(r"\n{3,}", "\n\n", text)
    # 연속된 공백/탭은 하나로
    text = re.sub(r"[ \t]{2,}", " ", text)
    return text.strip()
