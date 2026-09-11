import logging
import time

from openai import APIConnectionError, APIStatusError, APITimeoutError, OpenAI, RateLimitError

from app.core.config import get_settings
from app.core.exceptions import ProcessingError

logger = logging.getLogger(__name__)

# 잠시 뒤 다시 시도하면 성공할 수 있는 오류들. 인증 실패·잘못된 요청은 재시도해도 소용없다.
_RETRYABLE = (RateLimitError, APITimeoutError, APIConnectionError)


def _client() -> OpenAI:
    settings = get_settings()
    if not settings.openai_api_key:
        raise ProcessingError("OPENAI_API_KEY 가 설정되지 않았습니다")
    # max_retries=0: SDK 의 자동 재시도를 끄고 아래 _create_with_retry 에서 직접 다룬다.
    # 두 겹으로 재시도하면 실제 대기 시간이 설정값과 크게 달라진다.
    return OpenAI(api_key=settings.openai_api_key, max_retries=0)


def create_embeddings(texts: list[str]) -> list[list[float]]:
    """
    청크 목록의 임베딩을 순서 그대로 만들어 돌려준다.

    청크를 하나씩 보내지 않고 배치로 묶는다. 청크 200개짜리 자료를 하나씩 보내면
    왕복 지연만 200번 쌓인다.

    하나라도 실패하면 ProcessingError 를 던진다. 일부만 성공한 결과를 돌려주면
    자료의 일부만 검색되는 상태가 되는데, 이는 "검색 결과 없음"보다 나쁘다.
    사용자는 자료 전체가 반영됐다고 믿기 때문이다.
    """
    if not texts:
        return []

    settings = get_settings()
    client = _client()
    embeddings: list[list[float]] = []

    for offset in range(0, len(texts), settings.embedding_batch_size):
        batch = texts[offset : offset + settings.embedding_batch_size]
        embeddings.extend(_embed_batch(client, batch, offset))

    return embeddings


def _embed_batch(client: OpenAI, batch: list[str], offset: int) -> list[list[float]]:
    settings = get_settings()
    response = _create_with_retry(client, batch)

    if len(response.data) != len(batch):
        raise ProcessingError(
            f"임베딩 개수가 청크 수와 다릅니다: 요청 {len(batch)}개, 응답 {len(response.data)}개"
        )

    # 응답이 요청 순서대로 온다고 가정하지 않고 index 로 정렬한다.
    # 청크 순서가 어긋나면 검색 결과에 엉뚱한 본문이 붙는다.
    ordered = sorted(response.data, key=lambda item: item.index)
    for item in ordered:
        if len(item.embedding) != settings.embedding_dimensions:
            raise ProcessingError(
                f"임베딩 차원이 {settings.embedding_dimensions} 이 아닙니다: {len(item.embedding)}"
            )

    logger.info("임베딩 생성: %d~%d번 청크", offset, offset + len(batch) - 1)
    return [item.embedding for item in ordered]


def _create_with_retry(client: OpenAI, batch: list[str]):
    """
    일시적 오류(429 rate limit, 5xx, 타임아웃)에 대해 지수 백오프로 재시도한다.

    rate limit 은 동시에 여러 자료를 처리할 때 흔히 발생하고 대부분 잠시 뒤 풀린다.
    반면 인증 실패(401)·잘못된 요청(400)은 몇 번을 보내도 결과가 같으므로 바로 포기한다.
    """
    settings = get_settings()
    last_error: Exception | None = None

    for attempt in range(settings.embedding_max_retries):
        try:
            return client.embeddings.create(
                model=settings.embedding_model,
                input=batch,
                dimensions=settings.embedding_dimensions,
            )
        except RateLimitError as e:
            # 429 는 대부분 잠시 뒤 풀리는 rate limit 이지만, 크레딧 소진
            # (insufficient_quota)도 같은 429 로 온다. 후자는 몇 번을 더 보내도
            # 결과가 같으므로 백오프로 시간만 버리지 않고 바로 실패시킨다.
            if _is_quota_exhausted(e):
                raise ProcessingError(f"OpenAI 크레딧이 부족합니다: {e}") from e
            last_error = e
        except _RETRYABLE as e:
            last_error = e
        except APIStatusError as e:
            if e.status_code < 500:
                raise ProcessingError(f"임베딩 요청이 거부되었습니다: {e}") from e
            last_error = e

        wait = settings.embedding_retry_base_seconds * (2**attempt)
        logger.warning(
            "임베딩 요청 실패, %.1f초 후 재시도합니다 (%d/%d): %s",
            wait,
            attempt + 1,
            settings.embedding_max_retries,
            last_error,
        )
        time.sleep(wait)

    raise ProcessingError(f"임베딩 요청이 {settings.embedding_max_retries}회 모두 실패했습니다: {last_error}")


def _is_quota_exhausted(error: RateLimitError) -> bool:
    body = error.body if isinstance(error.body, dict) else {}
    return body.get("code") == "insufficient_quota" or body.get("type") == "insufficient_quota"
