// 백엔드가 LocalDateTime 을 타임존 없는 ISO 문자열로 내려준다(예: 2026-09-08T10:12:00).
// 목업처럼 날짜만 필요하므로 앞 10자만 쓴다. Date 로 파싱하면 브라우저 타임존이 끼어든다.
export function formatDate(isoString) {
  return isoString ? isoString.slice(0, 10) : '';
}

export function formatDateTime(isoString) {
  return isoString ? isoString.slice(0, 16).replace('T', ' ') : '';
}
