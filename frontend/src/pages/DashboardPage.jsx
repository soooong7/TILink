import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { findMyMaterials } from '../api/materials';
import { findMyTils, findRelatedTils, findTilsNeedingReview } from '../api/tils';
import Button from '../components/ui/Button';
import StatusBadge from '../components/ui/StatusBadge';
import { formatDate } from '../utils/format';
import styles from './DashboardPage.module.css';

const RECENT_LIMIT = 4;

/**
 * 학습 대시보드.
 *
 * <p>대시보드 전용 API 는 만들지 않았다. 필요한 값이 모두 기존 목록 API 로 나오고,
 * 집계를 백엔드에 두면 같은 숫자를 두 곳(여기와 목록 화면)에서 세게 된다.
 * 기록이 수천 건으로 늘어 목록 전체를 받는 것이 부담스러워지면 그때 집계 API 를 만든다.
 */
export default function DashboardPage() {
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  // 관련 학습은 위 세 호출이 끝나야 대상 TIL 을 알 수 있어 따로 받는다.
  const [related, setRelated] = useState({ loading: true, baseTil: null, item: null });

  useEffect(() => {
    let cancelled = false;

    // 세 호출은 서로 의존하지 않으므로 병렬로 보낸다. 순서대로 보내면 대기 시간이 그대로 쌓인다.
    Promise.all([findMyTils(), findMyMaterials(), findTilsNeedingReview()])
      .then(([tils, materials, needsReview]) => {
        if (!cancelled) setData({ tils, materials, needsReview });
      })
      .catch((err) => {
        if (!cancelled) setError(err.message);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!data) return undefined;

    // 임베딩이 없는 TIL 로 조회하면 백엔드가 409 로 거절한다. 준비된 것 중 가장 최근 것을 쓴다.
    const baseTil = data.tils.find((til) => til.embeddingReady) ?? null;
    if (!baseTil) {
      setRelated({ loading: false, baseTil: null, item: null });
      return undefined;
    }

    let cancelled = false;
    findRelatedTils(baseTil.id, 1)
      .then((items) => {
        if (!cancelled) setRelated({ loading: false, baseTil, item: items[0] ?? null });
      })
      // 관련 학습을 못 불러와도 대시보드 전체를 실패로 만들지 않는다. 이 섹션만 비운다.
      .catch(() => {
        if (!cancelled) setRelated({ loading: false, baseTil, item: null });
      });

    return () => {
      cancelled = true;
    };
  }, [data]);

  const stats = data ? summarize(data) : null;
  const reviewIds = data ? new Set(data.needsReview.map((til) => til.id)) : new Set();

  return (
    <>
      <section className={styles.hero}>
        <div className={styles.heroInner}>
          <div>
            <h1 className={styles.heroTitle}>학습 대시보드</h1>
            <p className={styles.heroSubtitle}>지금까지 쌓인 학습 기록을 한눈에 확인하세요</p>
          </div>
          {/* 업로드는 별도 페이지가 아니라 학습자료 목록의 모달이다. 목록으로 보내면서
              모달을 열어 달라는 신호를 라우터 state 로 넘긴다. */}
          <Button onClick={() => navigate('/materials', { state: { openUpload: true } })}>
            자료 업로드
          </Button>
        </div>
      </section>

      <div className="page">
        {error && <p className={styles.error}>{error}</p>}

        {!data && !error && <p className={styles.empty}>불러오는 중…</p>}

        {data && (
          <>
            <div className={styles.stats}>
              <Stat label="작성한 TIL" value={stats.tilCount} tone="blue" />
              <Stat label="업로드한 학습자료" value={stats.materialCount} tone="indigo" />
              <Stat label="학습한 기술 태그" value={stats.tagCount} tone="green" />
              <Stat label="복습 대상" value={stats.reviewCount} tone="amber" />
            </div>

            <div className={styles.columns}>
              <section className={styles.card}>
                <div className={styles.cardHead}>
                  <h2 className={styles.cardTitle}>최근 작성한 TIL</h2>
                  {data.tils.length > RECENT_LIMIT && (
                    <Link to="/tils" className={styles.more}>
                      전체 보기
                    </Link>
                  )}
                </div>

                {data.tils.length === 0 ? (
                  <p className={styles.sectionEmpty}>
                    아직 작성한 TIL 이 없습니다. 학습자료를 올리고 AI로 초안을 만들어보세요.
                  </p>
                ) : (
                  <ul className={styles.list}>
                    {data.tils.slice(0, RECENT_LIMIT).map((til) => (
                      <li key={til.id}>
                        <Link to={`/tils/${til.id}`} className={styles.row}>
                          <div className={styles.rowHead}>
                            <span className={styles.rowTitle}>{til.title}</span>
                            {reviewIds.has(til.id) && (
                              <span className={styles.reviewBadge}>복습 필요</span>
                            )}
                          </div>
                          <p className={styles.rowMeta}>
                            {til.subjectName}
                            <span className={styles.sep}>·</span>
                            {formatDate(til.createdAt)}
                          </p>
                        </Link>
                      </li>
                    ))}
                  </ul>
                )}
              </section>

              <section className={styles.card}>
                <div className={styles.cardHead}>
                  <h2 className={styles.cardTitle}>최근 학습자료</h2>
                  {data.materials.length > RECENT_LIMIT && (
                    <Link to="/materials" className={styles.more}>
                      전체 보기
                    </Link>
                  )}
                </div>

                {data.materials.length === 0 ? (
                  <p className={styles.sectionEmpty}>아직 업로드한 학습자료가 없습니다.</p>
                ) : (
                  <ul className={styles.list}>
                    {data.materials.slice(0, RECENT_LIMIT).map((material) => (
                      <li key={material.id}>
                        <Link to={`/materials/${material.id}`} className={styles.row}>
                          <div className={styles.rowHead}>
                            <span className={styles.rowTitle} title={material.title}>
                              {material.title}
                            </span>
                            <StatusBadge status={material.processingStatus} />
                          </div>
                          <p className={styles.rowMeta}>
                            {material.subject.name}
                            <span className={styles.sep}>·</span>
                            {formatDate(material.uploadedAt)}
                          </p>
                        </Link>
                      </li>
                    ))}
                  </ul>
                )}
              </section>
            </div>

            <RelatedPreview state={related} />

            <section className={styles.card}>
              <h2 className={styles.cardTitle}>과목별 학습 기록</h2>
              {stats.bySubject.length === 0 ? (
                <p className={styles.sectionEmpty}>TIL 을 작성하면 과목별 기록이 쌓입니다.</p>
              ) : (
                <ul className={styles.bars}>
                  {stats.bySubject.map((row) => (
                    <li key={row.name} className={styles.bar}>
                      <span className={styles.barLabel}>{row.name}</span>
                      <span className={styles.barTrack}>
                        {/* 가장 많은 과목을 100% 로 두는 상대 길이다. 절대 개수는 옆에 숫자로 적는다. */}
                        <span
                          className={styles.barFill}
                          style={{ width: `${(row.count / stats.bySubject[0].count) * 100}%` }}
                        />
                      </span>
                      <span className={styles.barCount}>{row.count}건</span>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </>
        )}
      </div>
    </>
  );
}

/**
 * 관련 학습 미리보기. 가장 최근 TIL 하나를 기준으로 가장 가까운 과거 기록 한 건만 보여준다.
 *
 * <p>보여줄 수 없는 이유가 세 가지(TIL 없음 / 임베딩 없음 / 겹치는 기록 없음)라 각각 다르게
 * 알린다. 전부 "관련 학습이 없습니다"로 뭉뚱그리면 사용자가 무엇을 해야 할지 알 수 없다.
 */
function RelatedPreview({ state }) {
  const { loading, baseTil, item } = state;

  return (
    <section className={styles.card}>
      <div className={styles.cardHead}>
        <h2 className={styles.cardTitle}>관련 학습</h2>
        {baseTil && (
          <Link to={`/tils/${baseTil.id}`} className={styles.more}>
            더 보기
          </Link>
        )}
      </div>

      {loading && <p className={styles.sectionEmpty}>불러오는 중…</p>}

      {!loading && !baseTil && (
        <p className={styles.sectionEmpty}>
          TIL 을 저장하면 과거에 공부한 내용 중 관련 있는 기록을 찾아드립니다.
        </p>
      )}

      {!loading && baseTil && !item && (
        <p className={styles.sectionEmpty}>
          아직 「{baseTil.title}」 과 겹치는 과거 기록이 없습니다. 기록이 쌓이면 나타납니다.
        </p>
      )}

      {!loading && baseTil && item && (
        <div className={styles.related}>
          <div className={styles.relatedFlow}>
            <span className={styles.relatedNow}>{baseTil.title}</span>
            <span className={styles.relatedArrow} aria-hidden="true">
              →
            </span>
            <Link to={`/tils/${item.id}`} className={styles.relatedPast}>
              {item.title}
            </Link>
          </div>
          <p className={styles.relatedMeta}>
            {/* 날짜·유사도는 모두 오른쪽(과거 기록) 기준이다. 그냥 날짜만 적으면
                왼쪽 TIL 의 작성일로 읽힌다. */}
            과거 기록
            <span className={styles.sep}>·</span>
            {item.date} 작성
            <span className={styles.sep}>·</span>
            유사도 {item.similarityScore.toFixed(2)}
            {item.matchedTags.length > 0 && (
              <>
                <span className={styles.sep}>·</span>
                겹친 태그 {item.matchedTags.join(', ')}
              </>
            )}
          </p>
        </div>
      )}
    </section>
  );
}

function Stat({ label, value, tone }) {
  return (
    <div className={`${styles.stat} ${styles[tone]}`}>
      <span className={styles.statLabel}>{label}</span>
      <strong className={styles.statValue}>{value}</strong>
    </div>
  );
}

/**
 * 목록 응답에서 대시보드 숫자를 계산한다.
 *
 * <p>목록 API 는 전체를 내려주므로 여기서 세는 값과 화면에 보이는 목록이 어긋날 일이 없다.
 */
function summarize({ tils, materials, needsReview }) {
  const tagCount = new Set(tils.flatMap((til) => til.tags)).size;

  const counts = new Map();
  tils.forEach((til) => counts.set(til.subjectName, (counts.get(til.subjectName) ?? 0) + 1));
  const bySubject = [...counts]
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count);

  return {
    tilCount: tils.length,
    materialCount: materials.length,
    tagCount,
    reviewCount: needsReview.length,
    bySubject,
  };
}
