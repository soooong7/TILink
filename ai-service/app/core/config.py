from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """
    .env 에서 읽어오는 서비스 설정.

    값을 os.environ 으로 직접 읽지 않고 한 곳에 모으는 이유는, 설정이 빠졌을 때
    기동 시점에 바로 실패시키기 위해서다. db_url 은 기본값이 없어서 .env 가
    없으면 앱이 뜨지 않는다.
    """

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    db_url: str
    ai_service_port: int = 8000

    # 5단계에서 사용한다. 아직 호출하는 코드가 없으므로 비어 있어도 된다.
    openai_api_key: str = ""


@lru_cache
def get_settings() -> Settings:
    # .env 파싱을 매번 하지 않도록 캐싱한다. 테스트에서 갈아끼울 땐 cache_clear() 를 쓴다.
    return Settings()
