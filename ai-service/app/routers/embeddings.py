from fastapi import APIRouter

from app.core.config import get_settings
from app.schemas.embedding import EmbeddingRequest, EmbeddingResponse
from app.services import embedding_service

router = APIRouter(prefix="/embeddings", tags=["embeddings"])


@router.post("", response_model=EmbeddingResponse)
def create_embedding(request: EmbeddingRequest) -> EmbeddingResponse:
    """
    임의의 텍스트를 임베딩한다.

    backend 가 TIL 을 저장할 때 본문 임베딩을 얻으려고 호출하는 범용 엔드포인트다.
    TIL 을 여기서 읽거나 쓰지 않으므로, 다른 텍스트에도 그대로 쓸 수 있다.
    """
    settings = get_settings()
    embedding = embedding_service.create_embeddings([request.text])[0]

    return EmbeddingResponse(
        embedding=embedding,
        dimensions=settings.embedding_dimensions,
        model=settings.embedding_model,
    )
