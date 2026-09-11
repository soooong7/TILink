/**
 * 초안 항목을 마크다운으로 조립한다. 형식이 두 가지다.
 *
 *   buildTilContent()      6항목 TIL (기획서 템플릿) — tils.content 에 저장
 *   buildDocumentMarkdown() 블로그 공유용 문서       — tils.document_markdown 에 저장
 *
 * 회고 3항목(새롭게 알게 된 점 / 어려웠던 점 / 오늘의 회고)은 두 형식이 공유한다.
 * 사용자가 편집 탭에서 고치면 양쪽에 함께 반영된다.
 *
 * <주의> 두 형식 모두 ai-service 의 to_til_markdown() / to_document_markdown() 과 같은
 * 모양이어야 한다. 사용자가 항목을 고치면 본문을 다시 만들어야 하는데 저장 API 는 조립된
 * 마크다운만 받기 때문에 조립이 화면 쪽에도 필요하다. 한쪽을 바꾸면 다른 쪽도 같이 바꾼다.
 */
export function buildTilContent(form) {
  const concepts = form.keyConcepts
    .filter((concept) => concept.name.trim() || concept.description.trim())
    .map((concept) => `### ${concept.name.trim()}\n${concept.description.trim()}`)
    .join('\n\n');

  return [
    `## 오늘 배운 내용\n\n${form.todayLearned.trim()}`,
    `## 핵심 개념\n\n${concepts}`,
    `## 실습 내용\n\n${form.practice.trim()}`,
    retrospective(form),
  ].join('\n\n');
}

export function buildDocumentMarkdown(form) {
  const sections = form.sections.filter(
    (section) => section.heading.trim() || section.bodyMarkdown.trim(),
  );

  const outline = sections.map((section) => `- ${section.heading.trim()}`).join('\n');
  const body = sections
    .map((section) => `## ${section.heading.trim()}\n\n${section.bodyMarkdown.trim()}`)
    .join('\n\n');

  return [`# ${form.title.trim()}`, `## 목차\n\n${outline}`, body, retrospective(form)].join('\n\n');
}

// 두 형식이 공유하는 회고 3항목. 한 곳에서 만들어야 양쪽이 갈라지지 않는다.
function retrospective(form) {
  return [
    `## 새롭게 알게 된 점\n\n${toBulletList(form.newLearnings)}`,
    `## 어려웠던 점\n\n${toBulletList(form.difficulties)}`,
    `## 오늘의 회고\n\n${form.reflection.trim()}`,
  ].join('\n\n');
}

// 여러 줄 입력을 마크다운 목록으로 바꾼다. 빈 줄은 버린다.
function toBulletList(text) {
  return text
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => (line.startsWith('- ') ? line : `- ${line}`))
    .join('\n');
}

// 서버가 준 문자열 배열을 한 줄에 하나씩 편집할 수 있는 텍스트로 바꾼다.
export function toLines(items) {
  return (items ?? []).join('\n');
}
