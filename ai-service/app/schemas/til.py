from datetime import datetime

from pydantic import BaseModel, Field, model_validator

from app.schemas.base import CamelModel


# ==========================================
# TIL 초안 생성
# ==========================================

class KeyConcept(CamelModel):
    name: str
    description: str


class DraftSection(CamelModel):
    """
    블로그용 문서의 본문 섹션 하나. 교안의 주제 하나에 대응한다.

    중첩 모델도 CamelModel 을 상속해야 한다. BaseModel 로 두면 바깥 모델만 camelCase 로
    나가고 이 안은 snake_case 로 남아, 호출자(Spring)가 필드를 못 읽는다.

    body_markdown 은 마크다운 원문이라 표·코드블록·소제목을 그대로 담을 수 있다.
    교안에는 비교표나 코드 예제가 많은데, 이걸 평문으로 풀어 쓰면 정보가 뭉개진다.
    """

    heading: str = Field(description="섹션 제목. '## ' 같은 마크다운 기호 없이 제목 텍스트만 쓴다.")
    body_markdown: str = Field(description=(
        "섹션 본문(마크다운). 소제목은 '###' 부터 쓰고 '##' 은 쓰지 않는다."
        " 비교·분류 내용은 표로, 코드는 언어를 명시한 코드블록으로 쓴다."
    ))


class TilDraftContent(BaseModel):
    """
    LLM 이 Structured Output 으로 채우는 스키마.

    한 번의 호출로 **두 가지 결과물**을 만든다.
      1) 6항목 TIL (today_learned / key_concepts / practice + 회고 3항목) — 학습 기록
      2) 블로그용 문서 (sections) — 공유용
    둘을 따로 호출하면 비용과 지연이 두 배가 되고, 같은 자료를 두 번 읽은 결과가
    서로 어긋날 수 있다.

    회고 3항목(new_learnings / difficulties / reflection)은 두 결과물이 공유한다.

    목차(outline)는 받지 않는다. 섹션 제목에서 그대로 만들면 되고, 따로 받으면
    목차와 실제 섹션이 어긋날 수 있다.

    필드 설명(Field description)은 그대로 JSON 스키마에 실려 모델에 전달되므로,
    프롬프트의 일부라고 보고 작성한다.
    """

    title: str = Field(description="TIL 제목. 자료의 주제 범위를 드러내는 30자 내외 제목.")

    # ---- 6항목 TIL (학습 기록) ----
    today_learned: str = Field(description="오늘 배운 내용. 자료 전체의 흐름을 3~5문장으로 정리한다.")
    key_concepts: list[KeyConcept] = Field(description="핵심 개념 3~6개. 각 개념은 이름과 2~3문장 설명으로 쓴다.")
    practice: str = Field(description="실습 내용. 자료에 실습·예제·코드가 있으면 무엇을 어떻게 했는지 정리하고, 없으면 '실습 내용 없음'이라고 쓴다.")

    # ---- 블로그용 문서 ----
    sections: list[DraftSection] = Field(description=(
        "블로그에 올릴 문서의 본문 섹션 4~8개. 교안에 나온 순서와 주제 구분을 따른다."
        " 위 '핵심 개념'의 요약과 달리, 여기서는 그 주제를 처음 보는 사람도 이해할 수 있을 만큼"
        " 표와 코드 예시를 포함해 자세히 쓴다."
    ))

    # ---- 두 결과물이 공유하는 회고 ----
    new_learnings: list[str] = Field(description="새롭게 알게 된 점 2~4개. 각 항목은 한 문장으로 쓴다.")
    difficulties: list[str] = Field(description="어려웠던 점 1~3개. 학습자가 헷갈릴 만한 지점을 자료 근거로 쓴다.")
    reflection: str = Field(description="오늘의 회고. 배운 내용을 앞으로 어디에 적용할지 2~3문장으로 쓴다.")

    suggested_tags: list[str] = Field(description="추천 태그 최대 6개. 'Spring AI', 'Docker' 같은 기술·주제명만 쓰고 문장은 쓰지 않는다.")


class TilDraftResponse(CamelModel):
    material_id: str
    title: str

    # 6항목 TIL 을 이루는 항목들 (화면의 '초안 편집' 탭)
    today_learned: str
    key_concepts: list[KeyConcept]
    practice: str

    # 블로그용 문서를 이루는 항목들 (화면의 '마크다운' 탭)
    outline: list[str]
    sections: list[DraftSection]

    # 두 형식이 공유하는 회고
    new_learnings: list[str]
    difficulties: list[str]
    reflection: str

    suggested_tags: list[str]

    # tils.content 에 저장할 6항목 마크다운
    content_markdown: str
    # tils.document_markdown 에 저장할 블로그용 문서
    document_markdown: str

    # 컨텍스트로 실제 사용한 청크 수 / 자료의 전체 청크 수.
    # 토큰 예산 때문에 잘렸는지를 호출자가 알 수 있어야 한다.
    used_chunk_count: int
    total_chunk_count: int
    model: str


# ==========================================
# 유사 TIL 검색
# ==========================================

class SimilarTilsRequest(CamelModel):
    # userId 는 필수다. ai-service 에는 인증이 없어서 이 값이 없으면
    # 다른 교육생의 TIL 이 "관련 학습"으로 노출된다. 신뢰 경계는 backend 다.
    user_id: str
    til_id: str | None = None
    embedding: list[float] | None = None
    limit: int = Field(default=5, ge=1, le=50)
    min_similarity: float | None = Field(default=None, ge=-1, le=1)

    @model_validator(mode="after")
    def exactly_one_source(self) -> "SimilarTilsRequest":
        if (self.til_id is None) == (self.embedding is None):
            raise ValueError("tilId 와 embedding 중 정확히 하나만 보내야 합니다")
        return self


class SimilarTil(CamelModel):
    til_id: str
    title: str
    created_at: datetime
    # 코사인 유사도(1 - 코사인거리). 1에 가까울수록 비슷하다.
    similarity_score: float


class SimilarTilsResponse(CamelModel):
    results: list[SimilarTil]
    count: int
