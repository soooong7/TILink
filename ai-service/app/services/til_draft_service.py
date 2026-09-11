import logging

import tiktoken
from openai import APIError, OpenAI
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.core.exceptions import ProcessingError
from app.models.material_chunk import MaterialChunk
from app.schemas.til import TilDraftContent, TilDraftResponse

logger = logging.getLogger(__name__)

_SYSTEM_PROMPT = """\
너는 교육생의 학습자료(교안)를 읽고 TIL(Today I Learned) 초안을 쓰는 도우미다.

한 번에 두 가지 결과물을 만든다.
1) 학습 기록: 오늘 배운 내용 / 핵심 개념 / 실습 내용 — 본인이 다시 볼 짧은 요약
2) 공유용 문서: 주제별 섹션 — 기술 블로그(velog, 티스토리)에 그대로 붙여넣을 문서
같은 자료를 정리한 결과이므로 둘의 내용이 서로 어긋나면 안 된다.

규칙:
- 반드시 한국어로 쓴다.
- 주어진 자료에 있는 내용만 쓴다. 자료에 없는 내용을 지어내지 않는다.
- 자료에 근거가 없으면 항목을 비우지 말고 "자료에서 확인되지 않음"처럼 사실대로 쓴다.
- 요약만 나열하지 말고, 그 주제를 처음 보는 사람이 이해할 수 있게 설명한다.
- 비교·분류·목록형 내용(문제와 원인, 옵션별 차이, 용어 정리 등)은 표로 정리한다.
- 코드·설정 예시가 자료에 있으면 언어를 명시한 코드블록으로 옮긴다.
- 섹션 본문의 소제목은 '###' 부터 쓴다. '#' 과 '##' 은 서비스가 붙이므로 쓰지 않는다.
- 담백한 서술체로 쓴다. 마케팅 문구나 과장된 표현("놀라운", "혁신적인")은 쓰지 않는다.
"""


def generate_draft(db: Session, material_id: str) -> TilDraftResponse:
    """
    자료의 청크를 컨텍스트로 넣어 TIL 초안을 생성한다.

    청크는 chunk_index 순서대로 넣는다. 교안은 앞에서 뒤로 흐름이 있어서
    순서가 섞이면 "오늘 배운 내용"의 서술 순서도 흐트러진다.
    """
    settings = get_settings()
    chunks = _load_chunks(db, material_id)
    if not chunks:
        raise LookupError(material_id)

    context, used = _build_context(chunks, settings.til_draft_max_input_tokens)
    content = _call_llm(context)

    logger.info(
        "TIL 초안 생성 완료: %s (청크 %d/%d개 사용, 모델 %s)",
        material_id, used, len(chunks), settings.chat_model,
    )

    return TilDraftResponse(
        material_id=material_id,
        title=content.title,
        today_learned=content.today_learned,
        key_concepts=content.key_concepts,
        practice=content.practice,
        outline=[section.heading for section in content.sections],
        sections=content.sections,
        new_learnings=content.new_learnings,
        difficulties=content.difficulties,
        reflection=content.reflection,
        suggested_tags=content.suggested_tags,
        content_markdown=to_til_markdown(content),
        document_markdown=to_document_markdown(content),
        used_chunk_count=used,
        total_chunk_count=len(chunks),
        model=settings.chat_model,
    )


def _load_chunks(db: Session, material_id: str) -> list[str]:
    stmt = (
        select(MaterialChunk.content)
        .where(MaterialChunk.material_id == material_id)
        .order_by(MaterialChunk.chunk_index)
    )
    return list(db.scalars(stmt))


def _build_context(chunks: list[str], max_tokens: int) -> tuple[str, int]:
    """
    토큰 예산 안에서 앞에서부터 청크를 담는다.

    글자 수로 어림잡지 않고 토크나이저로 세는 이유는, 예산을 넘기면 API 가 400 을
    돌려주기 때문이다. 한국어는 토큰당 글자 수 편차가 커서 어림짐작이 잘 빗나간다.
    """
    encoding = tiktoken.get_encoding("o200k_base")
    parts: list[str] = []
    total = 0

    for index, chunk in enumerate(chunks):
        tokens = len(encoding.encode(chunk))
        if total + tokens > max_tokens:
            logger.warning(
                "토큰 예산(%d)을 넘어 청크 %d개에서 잘랐습니다 (전체 %d개)",
                max_tokens, index, len(chunks),
            )
            break
        parts.append(chunk)
        total += tokens

    return "\n\n".join(parts), len(parts)


def _call_llm(context: str) -> TilDraftContent:
    """
    Structured Output 으로 고정된 스키마를 채워 받는다.

    자유 텍스트를 받아 파싱하지 않는 이유는, 형식이 조금만 흔들려도 파싱이 깨지고
    그 결과가 그대로 DB 에 저장되기 때문이다. 스키마를 강제하면 파싱 단계가 사라진다.
    """
    settings = get_settings()
    if not settings.openai_api_key:
        raise ProcessingError("OPENAI_API_KEY 가 설정되지 않았습니다")

    client = OpenAI(api_key=settings.openai_api_key, timeout=settings.chat_timeout_seconds)

    try:
        completion = client.chat.completions.parse(
            model=settings.chat_model,
            messages=[
                {"role": "system", "content": _SYSTEM_PROMPT},
                {"role": "user", "content": f"다음 학습자료를 바탕으로 TIL 초안을 작성해줘.\n\n{context}"},
            ],
            response_format=TilDraftContent,
        )
    except APIError as e:
        raise ProcessingError(f"TIL 초안 생성 요청이 실패했습니다: {e}") from e

    parsed = completion.choices[0].message.parsed
    if parsed is None:
        # 길이 제한이나 안전 필터로 스키마를 못 채운 경우다. 빈 값을 저장하느니 실패시킨다.
        reason = completion.choices[0].finish_reason
        raise ProcessingError(f"TIL 초안을 스키마대로 받지 못했습니다 (finish_reason={reason})")

    return parsed


def to_til_markdown(content: TilDraftContent) -> str:
    """
    6항목 TIL(기획서 템플릿)을 마크다운으로 조립한다. tils.content 에 저장되는 값이다.

    <주의> 이 형식은 화면(frontend/src/utils/tilMarkdown.js)에도 같은 모양으로 있다.
    사용자가 항목을 고치면 본문을 다시 만들어야 하는데 저장 API 는 조립된 마크다운만
    받기 때문이다. 한쪽을 바꾸면 다른 쪽도 같이 바꾼다.
    """
    concepts = "\n\n".join(f"### {c.name}\n{c.description}" for c in content.key_concepts)

    return (
        f"## 오늘 배운 내용\n\n{content.today_learned}\n\n"
        f"## 핵심 개념\n\n{concepts}\n\n"
        f"## 실습 내용\n\n{content.practice}\n\n"
        f"{_retrospective_markdown(content)}"
    )


def to_document_markdown(content: TilDraftContent) -> str:
    """
    블로그 공유용 문서를 마크다운으로 조립한다. tils.document_markdown 에 저장된다.

    그대로 붙여넣을 수 있도록 제목(#)과 목차부터 만든다. 목차는 섹션 제목에서
    만들기 때문에 본문과 어긋날 수 없다.
    """
    outline = "\n".join(f"- {section.heading}" for section in content.sections)
    body = "\n\n".join(
        f"## {section.heading}\n\n{section.body_markdown.strip()}" for section in content.sections
    )

    return (
        f"# {content.title}\n\n"
        f"## 목차\n\n{outline}\n\n"
        f"{body}\n\n"
        f"{_retrospective_markdown(content)}"
    )


def _retrospective_markdown(content: TilDraftContent) -> str:
    """두 형식이 공유하는 회고 3항목. 한 곳에서 만들어야 양쪽이 갈라지지 않는다."""
    new_learnings = "\n".join(f"- {item}" for item in content.new_learnings)
    difficulties = "\n".join(f"- {item}" for item in content.difficulties)

    return (
        f"## 새롭게 알게 된 점\n\n{new_learnings}\n\n"
        f"## 어려웠던 점\n\n{difficulties}\n\n"
        f"## 오늘의 회고\n\n{content.reflection}\n"
    )
