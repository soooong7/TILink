import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { findAllSubjects } from '../api/subjects';
import { findMyTils } from '../api/tils';
import Tag from '../components/ui/Tag';
import { formatDate } from '../utils/format';
import styles from './TilListPage.module.css';

const EMPTY_FILTERS = { from: '', to: '', subjectId: '', tag: '' };

export default function TilListPage() {
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [subjects, setSubjects] = useState([]);

  // 어떤 조건으로 받은 결과인지 함께 담아 두면, 조건이 바뀐 직후를 렌더 시점에
  // "로딩 중"으로 판별할 수 있어 이펙트에서 상태를 초기화할 필요가 없다.
  const requestKey = JSON.stringify(filters);
  const [result, setResult] = useState({ requestKey: null, tils: [], error: '' });

  useEffect(() => {
    let cancelled = false;
    findMyTils(filters)
      .then((tils) => {
        if (!cancelled) setResult({ requestKey, tils, error: '' });
      })
      .catch((err) => {
        if (!cancelled) setResult({ requestKey, tils: [], error: err.message });
      });
    // 요청이 끝나기 전에 필터가 또 바뀌면 늦게 온 응답을 버린다.
    return () => {
      cancelled = true;
    };
  }, [filters, requestKey]);

  // 과목 목록은 필터가 바뀌어도 다시 받을 필요가 없다.
  useEffect(() => {
    findAllSubjects().then(setSubjects).catch(() => setSubjects([]));
  }, []);

  const loading = result.requestKey !== requestKey;
  const { tils, error } = loading ? { tils: [], error: '' } : result;

  const updateFilter = (name, value) => setFilters((prev) => ({ ...prev, [name]: value }));
  const hasFilters = Object.values(filters).some(Boolean);

  return (
    <div className="page">
      <div className={styles.head}>
        <h1 className={styles.title}>TIL</h1>
        <p className={styles.subtitle}>지금까지 정리한 학습 기록입니다</p>
      </div>

      <div className={styles.filters}>
        <label className={styles.filter}>
          <span className={styles.filterLabel}>작성일</span>
          <span className={styles.dateRange}>
            <input
              type="date"
              className={styles.control}
              value={filters.from}
              onChange={(event) => updateFilter('from', event.target.value)}
              aria-label="시작일"
            />
            <span className={styles.tilde}>~</span>
            <input
              type="date"
              className={styles.control}
              value={filters.to}
              onChange={(event) => updateFilter('to', event.target.value)}
              aria-label="종료일"
            />
          </span>
        </label>

        <label className={styles.filter}>
          <span className={styles.filterLabel}>과목</span>
          <select
            className={styles.control}
            value={filters.subjectId}
            onChange={(event) => updateFilter('subjectId', event.target.value)}
          >
            <option value="">전체 과목</option>
            {subjects.map((subject) => (
              <option key={subject.id} value={subject.id}>
                {subject.name}
              </option>
            ))}
          </select>
        </label>

        <label className={styles.filter}>
          <span className={styles.filterLabel}>태그</span>
          {/* 태그는 이름이 정확히 일치해야 걸린다. 목록의 태그를 눌러 채우는 것이 가장 확실하다. */}
          <input
            className={styles.control}
            value={filters.tag}
            onChange={(event) => updateFilter('tag', event.target.value)}
            placeholder="예: Spring AI"
          />
        </label>

        {hasFilters && (
          <button type="button" className={styles.reset} onClick={() => setFilters(EMPTY_FILTERS)}>
            필터 초기화
          </button>
        )}
      </div>

      {error && <p className={styles.error}>{error}</p>}

      {loading ? (
        <p className={styles.empty}>불러오는 중…</p>
      ) : tils.length === 0 ? (
        <p className={styles.empty}>
          {hasFilters
            ? '조건에 맞는 TIL 이 없습니다.'
            : '아직 저장한 TIL 이 없습니다. 학습자료에서 AI로 TIL을 만들어보세요.'}
        </p>
      ) : (
        <ul className={styles.list}>
          {tils.map((til) => (
            <li key={til.id} className={styles.item}>
              <Link to={`/tils/${til.id}`} className={styles.card}>
                <div className={styles.cardHead}>
                  <span className={styles.cardTitle}>{til.title}</span>
                  <span className={styles.cardDate}>{formatDate(til.createdAt)}</span>
                </div>
                <p className={styles.cardMeta}>
                  {til.subjectName}
                  <span className={styles.sep}>·</span>
                  {til.materialTitle}
                </p>
              </Link>

              {til.tags.length > 0 && (
                <div className={styles.tags}>
                  {til.tags.map((tag) => (
                    // 태그를 누르면 그 태그로 필터링한다. 이름을 직접 입력하지 않아도 된다.
                    <button
                      key={tag}
                      type="button"
                      className={styles.tagButton}
                      onClick={() => updateFilter('tag', tag)}
                    >
                      <Tag>{tag}</Tag>
                    </button>
                  ))}
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
