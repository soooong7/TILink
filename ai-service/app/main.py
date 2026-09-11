import logging

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from app.core import storage
from app.core.config import get_settings
from app.core.exceptions import ProcessingError
from app.routers import embeddings, health, materials, tils

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)-5s %(name)s - %(message)s")

app = FastAPI(title="TILink AI Service", version="0.1.0")
app.include_router(health.router)
app.include_router(materials.router)
app.include_router(embeddings.router)
app.include_router(tils.router)


@app.exception_handler(ProcessingError)
def handle_processing_error(request: Request, exc: ProcessingError) -> JSONResponse:
    # OpenAI 호출 실패·설정 누락 등 외부 의존성 문제다. 호출자(backend) 입장에서는
    # 자기 요청이 잘못된 게 아니므로 4xx 가 아니라 502 로 알린다.
    logging.getLogger(__name__).error("요청 처리 실패: %s", exc)
    return JSONResponse(status_code=502, content={"detail": str(exc)})

storage.log_storage_root()


if __name__ == "__main__":
    # python -m app.main 으로도 뜨게 해 둔다. 평소에는 uvicorn 명령을 쓴다.
    import uvicorn

    uvicorn.run("app.main:app", host="0.0.0.0", port=get_settings().ai_service_port, reload=True)
