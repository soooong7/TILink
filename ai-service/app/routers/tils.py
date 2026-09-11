from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.exceptions import ConflictError
from app.schemas.til import SimilarTilsRequest, SimilarTilsResponse
from app.services import similarity_service

router = APIRouter(prefix="/tils", tags=["tils"])


@router.post("/similar", response_model=SimilarTilsResponse)
def find_similar(request: SimilarTilsRequest, db: Session = Depends(get_db)) -> SimilarTilsResponse:
    """
    임베딩(또는 tilId)과 유사한 TIL 을 유사도 순으로 돌려준다.

    순수 벡터 유사도만 계산한다. 태그 매칭을 섞는 하이브리드 랭킹은 backend 의 몫이다.
    """
    try:
        results = similarity_service.find_similar(db, request)
    except LookupError as e:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "TIL_NOT_FOUND") from e
    except ConflictError as e:
        raise HTTPException(status.HTTP_409_CONFLICT, str(e)) from e
    except ValueError as e:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_CONTENT, str(e)) from e

    return SimilarTilsResponse(results=results, count=len(results))
