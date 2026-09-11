from functools import lru_cache

from pydantic import model_validator
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

    # 학습자료 파일 저장 루트. backend 의 STORAGE_ROOT 와 같은 디렉터리를 가리켜야 한다.
    # materials.file_url 은 이 루트 기준 상대 키라서, 루트가 어긋나면 파일을 찾지 못한다.
    storage_root: str = "../backend/uploads"

    # 키가 없어도 서비스는 뜬다. 실제 처리 시점에 없으면 그 자료만 FAILED 로 끝낸다.
    openai_api_key: str = ""

    # ---- 임베딩 ----
    # 차원을 1536 으로 고정한 이유: material_chunks.embedding 이 vector(1536) 이고
    # HNSW 인덱스도 그 차원으로 만들어져 있다. 모델을 바꾸려면 마이그레이션이 필요하다.
    embedding_model: str = "text-embedding-3-small"
    embedding_dimensions: int = 1536
    # 청크를 하나씩 보내면 호출 수만큼 왕복 지연이 쌓인다. 묶어서 한 번에 보낸다.
    embedding_batch_size: int = 64
    # 429(rate limit)·5xx·타임아웃은 잠시 뒤 성공하는 경우가 많아 지수 백오프로 재시도한다.
    embedding_max_retries: int = 3
    embedding_retry_base_seconds: float = 1.0

    # ---- 청킹 ----
    # 검색 품질 기준으로 정한 값이다. 모델 입력 한계(8191토큰)와는 무관하다.
    # 청크가 크면 한 벡터에 여러 주제가 섞여 유사도 변별력이 떨어지고,
    # 작으면 문맥이 잘려 "이것", "위 그림" 같은 지시어만 남는다.
    chunk_size: int = 600
    # 경계에 걸친 문장이 양쪽 어디에도 온전히 남지 않는 것을 막는다. 10~20% 가 적당하고,
    # 더 키우면 중복된 내용의 벡터가 늘어 검색 결과가 같은 얘기로 채워진다.
    chunk_overlap: int = 100
    # 자투리가 의미 없이 짧게 남지 않도록 하는 하한.
    min_chunk_size: int = 200

    @model_validator(mode="after")
    def validate_chunking(self) -> "Settings":
        # overlap 이 chunk_size 이상이면 다음 청크 시작점이 앞으로 가지 못해 무한 루프가 된다.
        if self.chunk_overlap >= self.chunk_size:
            raise ValueError("chunk_overlap 은 chunk_size 보다 작아야 한다")
        if self.min_chunk_size > self.chunk_size:
            raise ValueError("min_chunk_size 는 chunk_size 보다 클 수 없다")
        return self


@lru_cache
def get_settings() -> Settings:
    # .env 파싱을 매번 하지 않도록 캐싱한다. 테스트에서 갈아끼울 땐 cache_clear() 를 쓴다.
    return Settings()
