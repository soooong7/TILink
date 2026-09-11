import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { findRelatedTils } from '../api/tils';
import Tag from './ui/Tag';
import styles from './RelatedTils.module.css';

/**
 * 현재 TIL 과 관련된 과거 학습 기록.
 *
 * 순서는 백엔드가 정한다(벡터 유사도 0.7 + 태그 일치 0.3). 화면은 그 순서를 그대로 쓰고,
 * "왜 관련 있다고 나왔는지"를 유사도와 겹친 태그로 보여준다.
 *
 * @param ready 본문 임베딩이 준비됐는지. 준비되지 않았으면 호출하지 않는다.
 *              (백엔드가 409 로 거절하므로 굳이 요청을 보낼 이유가 없다)
 */
export default function RelatedTils({ tilId, ready }) {
  const [result, setResult] = useState({ tilId: null, tils: [], error: '' });

  useEffect(() => {
    if (!ready) return undefined;

    let cancelled = false;
    findRelatedTils(tilId)
      .then((tils) => {
        if (!cancelled) setResult({ tilId, tils, error: '' });
      })
      .catch((err) => {
        if (!cancelled) setResult({ tilId, tils: [], error: err.message });
      });
    // 다른 TIL 로 이동하면 늦게 온 응답을 버린다.
    return () => {
      cancelled = true;
    };
  }, [tilId, ready]);

  if (!ready) {
    return null;
  }

  const loading = result.tilId !== tilId;
  const { tils, error } = loading ? { tils: [], error: '' } : result;

  return (
    <section className={styles.section}>
      <h2 className={styles.title}>관련 학습</h2>
      <p className={styles.subtitle}>이 TIL 과 내용이 겹치는 과거 기록입니다</p>

      {error && <p className={styles.error}>{error}</p>}

      {loading && <p className={styles.empty}>관련 학습을 찾는 중…</p>}

      {!loading && !error && tils.length === 0 && (
        <p className={styles.empty}>아직 관련된 TIL 이 없습니다. 기록이 쌓이면 여기에 나타납니다.</p>
      )}

      <ul className={styles.list}>
        {tils.map((til) => (
          <li key={til.id} className={styles.item}>
            <Link to={`/tils/${til.id}`} className={styles.card}>
              <div className={styles.head}>
                <span className={styles.cardTitle}>{til.title}</span>
                {/* 기획서 9장 형식대로 코사인 유사도 원본을 소수점 둘째 자리까지 보여준다. */}
                <span className={styles.score}>유사도 {til.similarityScore.toFixed(2)}</span>
              </div>
              <span className={styles.date}>{til.date}</span>
            </Link>

            {til.matchedTags.length > 0 && (
              <div className={styles.tags}>
                <span className={styles.tagsLabel}>겹친 태그</span>
                {til.matchedTags.map((tag) => (
                  <Tag key={tag}>{tag}</Tag>
                ))}
              </div>
            )}
          </li>
        ))}
      </ul>
    </section>
  );
}
