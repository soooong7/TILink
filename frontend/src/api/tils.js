import { client } from './client';

/**
 * POST /api/materials/{id}/til-draft -> TilDraftResponse
 *
 * 저장하지 않고 초안만 돌려준다. LLM 호출이라 20초 안팎 걸리므로 화면에서
 * 진행 단계를 보여줘야 한다.
 */
export async function createTilDraft(materialId) {
  const { data } = await client.post(`/materials/${materialId}/til-draft`);
  return data;
}

/**
 * POST /api/tils -> TilResponse
 *
 * content 는 6항목 TIL, document 는 블로그 공유용 문서다. 백엔드가 두 값을 각각
 * tils.content / tils.document_markdown 에 저장한다.
 */
export async function createTil({ materialId, title, content, document, tags }) {
  const { data } = await client.post('/tils', { materialId, title, content, document, tags });
  return data;
}

// GET /api/tils/{id} -> TilResponse
// 서버가 이 호출 시점에 최종 열람 일시(lastReviewedAt)를 갱신한다.
export async function findTil(tilId) {
  const { data } = await client.get(`/tils/${tilId}`);
  return data;
}

/**
 * GET /api/tils -> [TilSummaryResponse]
 *
 * 필터는 모두 선택이다. 값이 비어 있으면 파라미터 자체를 보내지 않는다
 * (빈 문자열을 보내면 백엔드가 "빈 태그로 검색"처럼 해석할 여지가 생긴다).
 *
 * @param {{ from?: string, to?: string, subjectId?: string, tag?: string }} filters
 *        from·to 는 'YYYY-MM-DD' 형식의 작성일 범위이며 양끝을 포함한다.
 */
export async function findMyTils(filters = {}) {
  const params = Object.fromEntries(
    Object.entries(filters).filter(([, value]) => value),
  );

  const { data } = await client.get('/tils', { params });
  return data;
}

/**
 * GET /api/tils/{id}/related -> { relatedTils: [...] }
 *
 * 백엔드가 ai-service 의 벡터 유사도 결과에 태그 일치를 결합해 정렬한 목록이다.
 * 본문 임베딩이 없는 TIL 은 409(TIL_EMBEDDING_NOT_READY)로 거절되므로,
 * 화면에서는 embeddingReady 를 먼저 확인하고 호출한다.
 */
export async function findRelatedTils(tilId, limit) {
  const { data } = await client.get(`/tils/${tilId}/related`, {
    params: limit ? { limit } : undefined,
  });
  return data.relatedTils;
}
