import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { deleteTil, findTil } from '../api/tils';
import RelatedTils from '../components/RelatedTils';
import Button from '../components/ui/Button';
import ConfirmDialog from '../components/ui/ConfirmDialog';
import MarkdownSource from '../components/ui/MarkdownSource';
import MarkdownView from '../components/ui/MarkdownView';
import Tabs from '../components/ui/Tabs';
import Tag from '../components/ui/Tag';
import { formatDateTime } from '../utils/format';
import styles from './TilDetailPage.module.css';

/**
 * 저장된 TIL 은 두 형식으로 남는다.
 *   TIL 기록 — 6항목 템플릿(content). 항상 있다.
 *   미리보기·마크다운 — 블로그 공유용 문서(document). 문서 없이 저장된 TIL 은 null 이라 탭을 숨긴다.
 */
const TIL_TAB = { value: 'til', label: 'TIL 기록' };
const DOCUMENT_TABS = [
  { value: 'preview', label: '미리보기' },
  { value: 'markdown', label: '마크다운' },
];

/**
 * 저장된 TIL 상세.
 *
 * 이 화면을 열면 백엔드가 최종 열람 일시를 갱신한다(복습 알림 기준값). 조회만 해도
 * 서버 상태가 바뀌는 API 라, 화면에서 불필요하게 여러 번 호출하지 않는다.
 */
export default function TilDetailPage() {
  const { tilId } = useParams();
  const navigate = useNavigate();
  const [result, setResult] = useState({ tilId: null, til: null, error: '' });
  const [tab, setTab] = useState('til');
  const [confirming, setConfirming] = useState(false);

  useEffect(() => {
    let cancelled = false;
    findTil(tilId)
      .then((til) => {
        if (!cancelled) setResult({ tilId, til, error: '' });
      })
      .catch((err) => {
        if (!cancelled) setResult({ tilId, til: null, error: err.message });
      });
    return () => {
      cancelled = true;
    };
  }, [tilId]);

  const loading = result.tilId !== tilId;
  const { til, error } = loading ? { til: null, error: '' } : result;

  const handleDelete = async () => {
    await deleteTil(tilId);
    // 방금 지운 TIL 의 상세로 되돌아오지 않도록 히스토리에서 이 화면을 치운다.
    navigate('/tils', { replace: true });
  };

  return (
    <div className="page">
      <Link to="/materials" className={styles.back}>
        ← 학습자료 목록
      </Link>

      {error && <p className={styles.error}>{error}</p>}
      {loading && <p>불러오는 중…</p>}

      {til && (
        <>
          <div className={styles.head}>
            <h1 className={styles.title}>{til.title}</h1>
            <div className={styles.headActions}>
              <Button variant="secondary" onClick={() => navigate(`/tils/${tilId}/edit`)}>
                수정
              </Button>
              <Button variant="secondary" onClick={() => setConfirming(true)}>
                삭제
              </Button>
            </div>
          </div>

          <div className={styles.meta}>
            <span>{formatDateTime(til.createdAt)}</span>
            <span className={styles.sep}>·</span>
            <span>{til.material.subjectName}</span>
            <span className={styles.sep}>·</span>
            <Link to={`/materials/${til.material.id}`} className={styles.link}>
              {til.material.title}
            </Link>
          </div>

          {til.tags.length > 0 && (
            <div className={styles.tags}>
              {til.tags.map((tag) => (
                <Tag key={tag}>{tag}</Tag>
              ))}
            </div>
          )}

          {/* 임베딩이 없으면 관련 학습 조회를 할 수 없다. 사용자가 이유를 알 수 있게 알린다. */}
          {!til.embeddingReady && (
            <p className={styles.notice}>
              본문 임베딩이 아직 없어 관련 학습 조회를 사용할 수 없습니다. TIL 을 수정해 저장하면
              다시 시도합니다.
            </p>
          )}

          <div className={styles.card}>
            <Tabs
              tabs={til.document ? [TIL_TAB, ...DOCUMENT_TABS] : [TIL_TAB]}
              value={tab}
              onChange={setTab}
            />
            <div className={styles.pane}>
              {tab === 'til' && <MarkdownView markdown={til.content} />}
              {tab === 'preview' && <MarkdownView markdown={til.document} />}
              {tab === 'markdown' && <MarkdownSource markdown={til.document} />}
            </div>
          </div>

          <RelatedTils tilId={til.id} ready={til.embeddingReady} />

          {confirming && (
            <ConfirmDialog
              title="이 TIL 을 삭제할까요?"
              description={`"${til.title}" 이 삭제됩니다. 되돌릴 수 없습니다.\n학습자료와 원본 PDF 는 그대로 남습니다.`}
              onConfirm={handleDelete}
              onClose={() => setConfirming(false)}
            />
          )}
        </>
      )}
    </div>
  );
}
