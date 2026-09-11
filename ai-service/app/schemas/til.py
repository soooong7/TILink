from datetime import datetime

from pydantic import BaseModel, Field, model_validator

from app.schemas.base import CamelModel


# ==========================================
# TIL 초안 생성
# ==========================================

class KeyConcept(BaseModel):
    name: str
    description: str


class TilDraftContent(BaseModel):
    """
    LLM 이 Structured Output 으로 채우는 스키마.

    응답 모델(TilDraftResponse)과 분리한 이유는, 마크다운 조립·사용 청크 수 같은
    값은 LLM 이 아니라 서비스가 채우기 때문이다. 조립까지 LLM 에 맡기면 형식이
    매번 흔들려서 "일관된 학습 기록"이라는 서비스 전제가 깨진다.

    필드 설명(Field description)은 그대로 JSON 스키마에 실려 모델에 전달되므로,
    프롬프트의 일부라고 보고 작성한다.
    """

    title: str = Field(description="TIL 제목. 자료의 주제를 20자 내외로 요약한다.")
    today_learned: str = Field(description="오늘 배운 내용. 자료 전체의 흐름을 3~5문장으로 정리한다.")
    key_concepts: list[KeyConcept] = Field(description="핵심 개념 3~6개. 각 개념은 이름과 2~3문장 설명으로 쓴다.")
    practice: str = Field(description="실습 내용. 자료에 실습·예제·코드가 있으면 무엇을 어떻게 했는지 정리하고, 없으면 '실습 내용 없음'이라고 쓴다.")
    new_learnings: list[str] = Field(description="새롭게 알게 된 점 2~4개. 각 항목은 한 문장으로 쓴다.")
    difficulties: list[str] = Field(description="어려웠던 점 1~3개. 학습자가 헷갈릴 만한 지점을 자료 근거로 쓴다.")
    reflection: str = Field(description="오늘의 회고. 배운 내용을 앞으로 어디에 적용할지 2~3문장으로 쓴다.")
    suggested_tags: list[str] = Field(description="추천 태그 최대 5개. 'Spring AI', 'Docker' 같은 기술·주제명만 쓰고 문장은 쓰지 않는다.")


class TilDraftResponse(CamelModel):
    material_id: str
    title: str
    today_learned: str
    key_concepts: list[KeyConcept]
    practice: str
    new_learnings: list[str]
    difficulties: list[str]
    reflection: str
    suggested_tags: list[str]
    # tils.content 에 그대로 저장할 수 있는 마크다운 본문
    content_markdown: str
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
