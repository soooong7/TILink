from pydantic import BaseModel


class ProcessAcceptedResponse(BaseModel):
    """
    처리 요청을 접수했다는 응답. 처리가 끝났다는 뜻이 아니다.

    실제 처리는 백그라운드에서 돌기 때문에, 호출한 쪽(backend)은 이 응답을 받은
    뒤 GET /api/materials/{id} 의 processingStatus 로 진행 상황을 확인한다.
    """

    material_id: str
    status: str
