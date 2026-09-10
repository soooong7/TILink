from fastapi import APIRouter

router = APIRouter(tags=["health"])


@router.get("/health")
def health() -> dict[str, str]:
    # DB 연결까지 확인하지는 않는다. 프로세스가 살아 있는지만 보는 용도다.
    return {"status": "UP"}
