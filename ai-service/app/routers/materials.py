from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.schemas.material import ProcessAcceptedResponse
from app.schemas.til import TilDraftResponse
from app.services import material_service, til_draft_service

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


@router.post("/{material_id}/til-draft", response_model=TilDraftResponse)
def create_til_draft(material_id: str, db: Session = Depends(get_db)) -> TilDraftResponse:
    """
    자료의 청크를 컨텍스트로 TIL 초안을 생성한다.

    처리(process)와 달리 동기 응답이다. 사용자가 초안을 기다리는 화면 앞에 있고,
    결과를 저장하지 않고 그대로 돌려주기 때문이다. 저장은 backend 의 몫이다.
    """
    if material_service.find_material(db, material_id) is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "MATERIAL_NOT_FOUND")

    try:
        return til_draft_service.generate_draft(db, material_id)
    except LookupError as e:
        # 청크가 없다 = 아직 분석 전이거나 분석에 실패한 자료다.
        raise HTTPException(
            status.HTTP_409_CONFLICT,
            "아직 분석되지 않은 자료입니다. 처리(process)를 먼저 완료해야 합니다",
        ) from e
