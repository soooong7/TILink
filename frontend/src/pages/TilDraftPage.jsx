import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { findMaterial } from '../api/materials';
import { createTil, createTilDraft } from '../api/tils';
import Button from '../components/ui/Button';
import MarkdownSource from '../components/ui/MarkdownSource';
import MarkdownView from '../components/ui/MarkdownView';
import TagChipInput from '../components/ui/TagChipInput';
import Tabs from '../components/ui/Tabs';
import { buildDocumentMarkdown, buildTilContent, toLines } from '../utils/tilMarkdown';
import styles from './TilDraftPage.module.css';

/**
 * 초안 생성 중 보여줄 단계 문구.
 *
 * 서버가 진행률을 알려주지 않으므로 경과 시간으로 넘긴다. 실측상 초안 생성은 20~40초
 * 걸리는데 한 문구로 버티면 멈춘 것처럼 보인다. 마지막 문구는 끝날 때까지 유지된다.
 */
const LOADING_STAGES = [
  { after: 0, text: '학습자료 분석 중…' },
  { after: 6000, text: '핵심 개념 정리 중…' },
  { after: 14000, text: 'TIL 초안 작성 중…' },
];

/**
 * 탭 세 개가 같은 초안의 서로 다른 면을 보여준다.
 *   초안 편집 — 6항목 TIL(기획서 템플릿). 저장되는 학습 기록이다.
 *   미리보기  — 공유용 문서를 렌더한 모습
 *   마크다운  — 공유용 문서의 원문. 블로그에 붙여넣는 값이다.
 * 회고 3항목은 두 형식이 공유하므로 편집 탭에서 고치면 문서에도 반영된다.
 */
const TABS = [
  { value: 'edit', label: '초안 편집' },
  { value: 'preview', label: '미리보기' },
  { value: 'markdown', label: '마크다운' },
];

export default function TilDraftPage() {
  const { materialId } = useParams();
  const navigate = useNavigate();

  const [material, setMaterial] = useState(null);
  const [draft, setDraft] = useState(null);
  const [form, setForm] = useState(null);
  const [tags, setTags] = useState([]);
  const [tab, setTab] = useState('edit');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [stage, setStage] = useState(LOADING_STAGES[0].text);

  // 초안 생성은 LLM 호출이라 비용이 든다. 화면당 한 번만 실행한다.
  // (StrictMode 는 개발 중 이펙트를 두 번 실행하므로 ref 로 막는다)
  const requested = useRef(false);
  const loading = !form && !error;

  useEffect(() => {
    findMaterial(materialId).then(setMaterial).catch(() => setMaterial(null));
  }, [materialId]);

  useEffect(() => {
    if (requested.current) return;
    requested.current = true;

    createTilDraft(materialId)
      .then((result) => {
        setDraft(result);
        setForm(toForm(result));
        setTags(result.suggestedTags ?? []);
      })
      .catch((err) => setError(err.message));
  }, [materialId]);

  // 단계 문구 타이머는 요청 이펙트와 분리한다. 한 이펙트에 두면 StrictMode 의 두 번째
  // 실행이 중복 요청 가드에 걸려 일찍 반환하고, 그 사이 첫 실행의 정리 함수가 타이머를
  // 지워 버려서 문구가 첫 단계에 멈춘다.
  useEffect(() => {
    if (!loading) return undefined;

    const timers = LOADING_STAGES.slice(1).map((item) =>
      setTimeout(() => setStage(item.text), item.after),
    );
    return () => timers.forEach(clearTimeout);
  }, [loading]);

  const updateField = (name, value) => setForm((prev) => ({ ...prev, [name]: value }));

  const updateConcept = (index, key, value) =>
    setForm((prev) => ({
      ...prev,
      keyConcepts: prev.keyConcepts.map((concept, i) =>
        i === index ? { ...concept, [key]: value } : concept,
      ),
    }));

  // 편집 결과를 두 형식으로 각각 조립한다. 회고 3항목이 양쪽에 함께 들어간다.
  const content = form ? buildTilContent(form) : '';
  const document = form ? buildDocumentMarkdown(form) : '';

  const handleSave = async () => {
    setSaving(true);
    setError('');

    try {
      const til = await createTil({
        materialId,
        title: form.title.trim(),
        content,
        document,
        tags,
      });
      navigate(`/tils/${til.id}`);
    } catch (err) {
      setError(err.message);
      setSaving(false);
    }
  };

  return (
    <div className="page">
      <p className={styles.crumbs}>
        <Link to="/materials">학습자료</Link>
        {material && (
          <>
            <span className={styles.sep}>/</span>
            <Link to={`/materials/${materialId}`}>{material.title}</Link>
          </>
        )}
        <span className={styles.sep}>/</span>
        <span>TIL 초안</span>
      </p>

      {error && <p className={styles.error}>{error}</p>}

      {loading && (
        <div className={styles.loading}>
          <span className={styles.spinner} aria-hidden="true" />
          <p className={styles.stage}>{stage}</p>
          <p className={styles.hint}>학습자료 분량에 따라 20~40초 걸립니다.</p>
        </div>
      )}

      {form && (
        <>
          <section className={styles.card}>
            <label className={styles.label} htmlFor="til-title">
              TIL 제목
            </label>
            <input
              id="til-title"
              className={styles.input}
              value={form.title}
              onChange={(event) => updateField('title', event.target.value)}
              maxLength={255}
            />

            <p className={styles.tagLabel}>
              추천 태그 <span className={styles.tagHint}>· AI가 자료에서 추출했어요</span>
            </p>
            <TagChipInput tags={tags} onChange={setTags} />
          </section>

          <section className={styles.card}>
            <Tabs tabs={TABS} value={tab} onChange={setTab} />

            {tab === 'edit' && (
              <div className={styles.editor}>
                {draft.usedChunkCount < draft.totalChunkCount && (
                  <p className={styles.notice}>
                    자료가 길어 앞부분 {draft.usedChunkCount}개 조각(전체 {draft.totalChunkCount}개)만
                    반영했습니다.
                  </p>
                )}

                <Field
                  label="오늘 배운 내용"
                  name="todayLearned"
                  value={form.todayLearned}
                  onChange={updateField}
                  rows={5}
                />

                <fieldset className={styles.concepts}>
                  <legend className={styles.label}>핵심 개념</legend>
                  {form.keyConcepts.map((concept, index) => (
                    // 순서가 바뀌지 않는 목록이라 index 를 키로 써도 안전하다.
                    <div key={index} className={styles.concept}>
                      <input
                        className={styles.input}
                        value={concept.name}
                        onChange={(event) => updateConcept(index, 'name', event.target.value)}
                        aria-label={`핵심 개념 ${index + 1} 이름`}
                      />
                      <textarea
                        className={styles.textarea}
                        rows={3}
                        value={concept.description}
                        onChange={(event) => updateConcept(index, 'description', event.target.value)}
                        aria-label={`핵심 개념 ${index + 1} 설명`}
                      />
                    </div>
                  ))}
                </fieldset>

                <Field
                  label="실습 내용"
                  name="practice"
                  value={form.practice}
                  onChange={updateField}
                  rows={4}
                />

                <p className={styles.shared}>
                  아래 세 항목은 마크다운 문서에도 함께 들어갑니다.
                </p>

                <Field
                  label="새롭게 알게 된 점"
                  name="newLearnings"
                  value={form.newLearnings}
                  onChange={updateField}
                  rows={4}
                  hint="한 줄에 하나씩 적으면 목록으로 저장됩니다."
                />
                <Field
                  label="어려웠던 점"
                  name="difficulties"
                  value={form.difficulties}
                  onChange={updateField}
                  rows={3}
                  hint="한 줄에 하나씩 적으면 목록으로 저장됩니다."
                />
                <Field
                  label="오늘의 회고"
                  name="reflection"
                  value={form.reflection}
                  onChange={updateField}
                  rows={4}
                />
              </div>
            )}

            {tab === 'preview' && (
              <div className={styles.pane}>
                <MarkdownView markdown={document} />
              </div>
            )}

            {tab === 'markdown' && (
              <div className={styles.pane}>
                <p className={styles.hint}>
                  아래 원문을 그대로 복사해 티스토리·velog·GitHub 등 마크다운을 지원하는 곳에
                  붙여넣으세요.
                </p>
                <MarkdownSource markdown={document} />
              </div>
            )}
          </section>

          <div className={styles.actions}>
            <Link to={`/materials/${materialId}`} className={styles.cancel}>
              취소
            </Link>
            <Button onClick={handleSave} disabled={saving}>
              {saving ? '저장 중…' : 'TIL 저장'}
            </Button>
          </div>
        </>
      )}
    </div>
  );
}

function Field({ label, name, value, onChange, rows, hint }) {
  return (
    <div className={styles.field}>
      <label className={styles.label} htmlFor={name}>
        {label}
      </label>
      {hint && <p className={styles.hint}>{hint}</p>}
      <textarea
        id={name}
        className={styles.textarea}
        rows={rows}
        value={value}
        onChange={(event) => onChange(name, event.target.value)}
      />
    </div>
  );
}

// 서버 응답을 폼 상태로 옮긴다. 문자열 배열은 줄 단위로 편집할 수 있게 펼친다.
function toForm(draft) {
  return {
    title: draft.title,
    // 6항목 TIL (편집 탭)
    todayLearned: draft.todayLearned,
    keyConcepts: draft.keyConcepts.map((concept) => ({ ...concept })),
    practice: draft.practice,
    // 공유용 문서 (미리보기·마크다운 탭). 화면에서 직접 고치지는 않는다.
    sections: draft.sections.map((section) => ({ ...section })),
    // 두 형식이 공유하는 회고
    newLearnings: toLines(draft.newLearnings),
    difficulties: toLines(draft.difficulties),
    reflection: draft.reflection,
  };
}
