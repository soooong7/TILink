from pydantic import Field

from app.schemas.base import CamelModel


class EmbeddingRequest(CamelModel):
    # 상한을 두는 이유: 임베딩 모델 입력 한계(8191토큰)를 크게 넘는 텍스트를 받아
    # API 단에서 400 을 맞느니, 요청 단계에서 걸러 내는 편이 원인이 분명하다.
    text: str = Field(min_length=1, max_length=32000)


class EmbeddingResponse(CamelModel):
    embedding: list[float]
    dimensions: int
    model: str
