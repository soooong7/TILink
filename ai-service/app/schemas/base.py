from pydantic import BaseModel, ConfigDict
from pydantic.alias_generators import to_camel


class CamelModel(BaseModel):
    """
    JSON 은 camelCase, 파이썬 코드는 snake_case 로 쓰기 위한 공통 베이스.

    이 API 의 호출자는 Spring Boot 라서 응답 필드가 camelCase 여야 DTO 매핑이
    자연스럽다. populate_by_name=True 라 파이썬에서는 snake_case 로 생성할 수 있다.
    """

    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)
