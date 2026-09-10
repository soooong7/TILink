from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.schemas.material import ProcessAcceptedResponse
from app.services import material_service

router = APIRouter(prefix="/materials", tags=["materials"])


@router.post(
    "/{material_id}/process",
    status_code=status.HTTP_202_ACCEPTED,
    response_model=ProcessAcceptedResponse,
)
def process(
    material_id: str,
    background_tasks: BackgroundTasks,
    db: Session = Depends(get_db),
) -> ProcessAcceptedResponse:
    """
    학습자료 처리를 요청한다.

    처리는 수십 초가 걸릴 수 있으므로 응답을 기다리게 하지 않고 202 로 바로
    접수만 알린다. 자료가 존재하는지는 여기서 미리 확인해서, 잘못된 ID 는
    백그라운드로 넘어가기 전에 404 로 돌려준다.
    """
    if material_service.find_material(db, material_id) is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="MATERIAL_NOT_FOUND")

    background_tasks.add_task(material_service.process_material, material_id)
    return ProcessAcceptedResponse(material_id=material_id, status="ACCEPTED")
