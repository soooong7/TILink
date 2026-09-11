import logging

from fastapi import FastAPI

from app.core import storage
from app.core.config import get_settings
from app.routers import health, materials

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)-5s %(name)s - %(message)s")

app = FastAPI(title="TILink AI Service", version="0.1.0")
app.include_router(health.router)
app.include_router(materials.router)

storage.log_storage_root()


if __name__ == "__main__":
    # python -m app.main 으로도 뜨게 해 둔다. 평소에는 uvicorn 명령을 쓴다.
    import uvicorn

    uvicorn.run("app.main:app", host="0.0.0.0", port=get_settings().ai_service_port, reload=True)
