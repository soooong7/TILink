import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { findTilsNeedingReview } from '../api/tils';
import { formatDate } from '../utils/format';
import styles from './ReviewPage.module.css';

/**
 * 경과일을 세 단계로 나눠 색을 달리한다. 목록이 길어지면 숫자만으로는 어느 것부터
 * 봐야 할지 판단하기 어렵기 때문이다. 위에서부터 오래된 순이라 색은 보조 신호다.
 */
const URGENT_DAYS = 30;
const WARNING_DAYS = 14;

function levelOf(days) {
  if (days >= URGENT_DAYS) return styles.urgent;
  if (days >= WARNING_DAYS) return styles.warning;
  return styles.normal;
}

/**
 * 복습이 필요한 TIL 목록.
 *
 * <p>항목을 누르면 TIL 상세로 간다. 상세를 여는 순간 서버가 최종 열람 일시를 갱신하므로,
 * 돌아와서 새로고침하면 그 TIL 은 목록에서 빠진다. "복습 완료" 버튼을 따로 두지 않은 이유는
 * 열어보지 않고 목록만 비우는 길을 만들지 않기 위해서다.
 */
export default function ReviewPage() {
  const [result, setResult] = useState({ loaded: false, tils: [], error: '' });

  useEffect(() => {
    let cancelled = false;
    findTilsNeedingReview()
      .then((tils) => {
        if (!cancelled) setResult({ loaded: true, tils, error: '' });
      })
      .catch((err) => {
        if (!cancelled) setResult({ loaded: true, tils: [], error: err.message });
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const { loaded, tils, error } = result;

  return (
    <div className="page">
      <div className={styles.head}>
        <h1 className={styles.title}>복습</h1>
        <p className={styles.subtitle}>
          7일 이상 다시 열어보지 않은 TIL 입니다. 오래 묵은 순서로 보여드려요.
        </p>
      </div>

      {error && <p className={styles.error}>{error}</p>}

      {!loaded && <p className={styles.empty}>불러오는 중…</p>}

      {loaded && !error && tils.length === 0 && (
        <p className={styles.empty}>
          복습이 밀린 TIL 이 없습니다. 기록을 잘 챙기고 계시네요.
        </p>
      )}

      {tils.length > 0 && (
        <>
          <p className={styles.count}>복습이 필요한 TIL {tils.length}개</p>
          <ul className={styles.list}>
            {tils.map((til) => (
              <li key={til.id} className={styles.item}>
                <Link to={`/tils/${til.id}`} className={styles.card}>
                  <div className={styles.cardHead}>
                    <span className={styles.cardTitle}>{til.title}</span>
                    <span className={`${styles.days} ${levelOf(til.daysSinceReview)}`}>
                      {til.daysSinceReview}일째
                    </span>
                  </div>
                  <p className={styles.cardMeta}>
                    {til.subjectName}
                    <span className={styles.sep}>·</span>
                    {/* 한 번도 열지 않았다면 보여줄 열람 기록이 없으므로 작성일을 쓴다. */}
                    {til.neverReviewed
                      ? `${formatDate(til.createdAt)} 작성 후 한 번도 열지 않음`
                      : `마지막 열람 ${formatDate(til.lastReviewedAt)}`}
                  </p>
                </Link>
              </li>
            ))}
          </ul>
          <p className={styles.hint}>
            TIL 을 열어보면 복습한 것으로 기록되어 이 목록에서 빠집니다.
          </p>
        </>
      )}
    </div>
  );
}
