import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { findTil, updateTil } from '../api/tils';
import Button from '../components/ui/Button';
import MarkdownView from '../components/ui/MarkdownView';
import Tabs from '../components/ui/Tabs';
import TagChipInput from '../components/ui/TagChipInput';
import styles from './TilEditPage.module.css';

/**
 * 저장된 TIL 수정.
 *
 * <p>초안 화면(TilDraftPage)은 "오늘 배운 내용 / 핵심 개념 / …" 항목별 입력을 쓰지만 여기는
 * 마크다운을 직접 고친다. 저장된 TIL 에는 조립된 마크다운만 남아 있고 항목 구조는 남지
 * 않기 때문이다. 항목별 입력을 되살리려면 본문을 거꾸로 파싱해야 하는데, 사용자가 자유롭게
 * 고친 마크다운에서 그게 항상 맞아떨어질 수는 없다.
 *
 * <p>본문(TIL 기록)이 실제로 바뀌면 서버가 임베딩을 다시 만든다. 임베딩이 없어 관련 학습
 * 조회를 못 하던 TIL 도 여기서 저장하면 다시 시도된다.
 */
export default function TilEditPage() {
  const { tilId } = useParams();
  const navigate = useNavigate();

  const [form, setForm] = useState(null);
  const [tab, setTab] = useState('content');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let cancelled = false;
    findTil(tilId)
      .then((til) => {
        if (cancelled) return;
        setForm({
          title: til.title,
          content: til.content,
          document: til.document,
          tags: til.tags,
          materialTitle: til.material.title,
        });
      })
      .catch((err) => {
        if (!cancelled) setError(err.message);
      });
    return () => {
      cancelled = true;
    };
  }, [tilId]);

  const updateField = (name, value) => setForm((prev) => ({ ...prev, [name]: value }));

  const handleSave = async () => {
    if (!form.title.trim() || !form.content.trim()) {
      setError('제목과 TIL 본문은 비울 수 없습니다.');
      return;
    }
    setError('');
    setSaving(true);
    try {
      await updateTil(tilId, {
        title: form.title.trim(),
        content: form.content,
        // 문서가 없던 TIL 은 계속 없는 채로 둔다. 빈 문자열을 보내면 서버가 "문서를
        // 지우겠다"는 뜻으로 받아들인다.
        document: form.document ?? undefined,
        tags: form.tags,
      });
      navigate(`/tils/${tilId}`, { replace: true });
    } catch (err) {
      setError(err.message);
      setSaving(false);
    }
  };

  // 공유 문서 없이 저장된 TIL 은 편집할 문서도 없으므로 탭을 숨긴다.
  const tabs = [
    { value: 'content', label: 'TIL 기록' },
    ...(form?.document ? [{ value: 'document', label: '공유 문서' }] : []),
    { value: 'preview', label: '미리보기' },
  ];

  return (
    <div className="page">
      <p className={styles.crumbs}>
        <Link to="/tils">TIL</Link>
        <span className={styles.sep}>/</span>
        <Link to={`/tils/${tilId}`}>{form?.title ?? '불러오는 중…'}</Link>
        <span className={styles.sep}>/</span>
        <span>수정</span>
      </p>

      {error && <p className={styles.error}>{error}</p>}

      {!form && !error && <p>불러오는 중…</p>}

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

            <p className={styles.label}>태그</p>
            <TagChipInput tags={form.tags} onChange={(tags) => updateField('tags', tags)} />
            <p className={styles.hint}>학습자료: {form.materialTitle}</p>
          </section>

          <section className={styles.card}>
            <Tabs tabs={tabs} value={tab} onChange={setTab} />

            {tab === 'content' && (
              <div className={styles.pane}>
                <p className={styles.hint}>
                  관련 학습 조회에 쓰이는 본문입니다. 내용을 바꾸면 임베딩을 다시 만듭니다.
                </p>
                <textarea
                  className={styles.textarea}
                  rows={24}
                  value={form.content}
                  onChange={(event) => updateField('content', event.target.value)}
                  aria-label="TIL 본문 마크다운"
                />
              </div>
            )}

            {tab === 'document' && (
              <div className={styles.pane}>
                <p className={styles.hint}>블로그에 붙여넣는 공유용 문서입니다.</p>
                <textarea
                  className={styles.textarea}
                  rows={24}
                  value={form.document}
                  onChange={(event) => updateField('document', event.target.value)}
                  aria-label="공유 문서 마크다운"
                />
              </div>
            )}

            {tab === 'preview' && (
              <div className={styles.pane}>
                <MarkdownView markdown={form.document || form.content} />
              </div>
            )}
          </section>

          <div className={styles.actions}>
            <Link to={`/tils/${tilId}`} className={styles.cancel}>
              취소
            </Link>
            <Button onClick={handleSave} disabled={saving}>
              {saving ? '저장 중…' : '저장'}
            </Button>
          </div>
        </>
      )}
    </div>
  );
}
