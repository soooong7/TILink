import { client } from './client';

/**
 * POST /api/materials (multipart/form-data)
 *
 * 백엔드가 subjectId·title 은 폼 필드로, 파일은 'file' 파트로 받는다.
 * title 이 비어 있으면 서버가 파일명에서 확장자를 뗀 값을 대신 쓴다.
 */
export async function uploadMaterial({ subjectId, title, file }) {
  const form = new FormData();
  form.append('subjectId', subjectId);
  if (title) {
    form.append('title', title);
  }
  form.append('file', file);

  // Content-Type 은 boundary 가 필요해 axios 가 직접 붙이도록 지정하지 않는다.
  const { data } = await client.post('/materials', form);
  return data;
}

// GET /api/materials?subjectId= -> [MaterialResponse]
export async function findMyMaterials(subjectId) {
  const { data } = await client.get('/materials', {
    params: subjectId ? { subjectId } : undefined,
  });
  return data;
}

// GET /api/materials/{id} -> MaterialResponse
export async function findMaterial(materialId) {
  const { data } = await client.get(`/materials/${materialId}`);
  return data;
}

/**
 * GET /api/materials/{id}/file -> PDF 바이너리
 *
 * 인증 헤더가 필요해서 <a href> 로는 받을 수 없다. blob 으로 받아 임시 URL 을 만든 뒤
 * 브라우저 다운로드를 일으킨다. 파일명은 응답 헤더를 파싱하지 않고 상세 응답의
 * originalFileName 을 그대로 쓴다(서버가 헤더에 넣는 값과 같은 값이다).
 */
export async function downloadMaterialFile(materialId, fileName) {
  const { data } = await client.get(`/materials/${materialId}/file`, {
    responseType: 'blob',
  });

  const url = URL.createObjectURL(data);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  // click() 직후 바로 해제해도 다운로드는 이미 시작된 상태다. 남겨두면 탭을 닫을 때까지 메모리를 잡는다.
  URL.revokeObjectURL(url);
}

/**
 * PATCH /api/materials/{id} -> MaterialResponse
 *
 * 보내지 않은 필드는 서버가 건드리지 않는다. 파일 교체는 지원하지 않는다.
 */
export async function updateMaterial(materialId, { title, subjectId }) {
  const { data } = await client.patch(`/materials/${materialId}`, { title, subjectId });
  return data;
}

/**
 * DELETE /api/materials/{id}
 *
 * 이 자료로 만든 TIL·청크와 저장된 PDF 까지 함께 사라진다. 되돌릴 수 없으므로
 * 화면에서 반드시 확인을 받은 뒤 호출한다.
 */
export async function deleteMaterial(materialId) {
  await client.delete(`/materials/${materialId}`);
}
