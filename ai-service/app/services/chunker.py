import re

# 자를 위치를 찾을 때 쓰는 경계. 앞쪽일수록 우선한다.
# 1) 문단 경계 2) 문장 끝(마침표/물음표/느낌표 + 공백) 3) 줄바꿈 4) 공백
_BOUNDARY_PATTERNS = [
    re.compile(r"\n\n"),
    re.compile(r"[.!?][\"')\]]?\s"),
    re.compile(r"\n"),
    re.compile(r"\s"),
]


def chunk_text(text: str, chunk_size: int, overlap: int, min_chunk_size: int) -> list[str]:
    """
    텍스트를 겹침이 있는 청크 목록으로 자른다.

    chunk_size 에서 무조건 끊지 않고 그 앞의 문단·문장 경계를 찾아 자른다. 문장
    한가운데서 잘린 청크는 임베딩이 엉뚱해지고, 나중에 사용자에게 근거로 보여줄 때도
    읽기 어렵다.

    청크 사이를 overlap 만큼 겹치는 이유는 경계에 걸친 문장이 양쪽 어디에도 온전히
    남지 않는 것을 막기 위해서다.

    :param chunk_size: 청크 하나의 목표 길이(문자 수)
    :param overlap: 앞 청크와 겹치는 길이
    :param min_chunk_size: 경계를 찾을 때 허용하는 최소 길이이자, 마지막 자투리 하한
    """
    if not text:
        return []

    chunks: list[str] = []
    start = 0
    length = len(text)

    while start < length:
        end = min(start + chunk_size, length)

        if end < length:
            end = _find_boundary(text, start, end, min_chunk_size)
            # 남은 꼬리가 하한보다 짧으면 따로 청크로 만들지 않고 이번 청크에 흡수한다.
            if length - end < min_chunk_size:
                end = length

        chunk = text[start:end].strip()
        if chunk:
            chunks.append(chunk)

        if end >= length:
            break

        # max(..., start + 1): 경계 탐색 결과가 짧아도 시작점은 반드시 전진시킨다.
        # (전진하지 않으면 같은 구간을 무한히 반복한다)
        start = max(end - overlap, start + 1)

    return chunks


def _find_boundary(text: str, start: int, hard_end: int, min_chunk_size: int) -> int:
    """
    start..hard_end 구간에서 가장 뒤쪽 경계를 찾아 자를 위치를 돌려준다.

    최소 길이(min_chunk_size)보다 앞에서는 자르지 않는다. 경계를 찾겠다고 청크가
    지나치게 짧아지면 문맥이 오히려 더 깨진다. 어떤 경계도 없으면 hard_end 그대로 자른다.
    """
    window_start = min(start + min_chunk_size, hard_end)
    window = text[window_start:hard_end]

    for pattern in _BOUNDARY_PATTERNS:
        matches = list(pattern.finditer(window))
        if matches:
            # 경계 문자 자체는 앞 청크에 포함시킨다 (문장 끝 마침표가 다음 청크로 넘어가지 않도록)
            return window_start + matches[-1].end()

    return hard_end
