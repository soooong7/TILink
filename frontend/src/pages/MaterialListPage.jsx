import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { findMyMaterials } from '../api/materials';
import { findAllSubjects } from '../api/subjects';
import MaterialUploadDialog from '../components/MaterialUploadDialog';
import Button from '../components/ui/Button';
import StatusBadge from '../components/ui/StatusBadge';
import Tag from '../components/ui/Tag';
import { formatDate } from '../utils/format';
import styles from './MaterialListPage.module.css';

export default function MaterialListPage() {
  const [subjectId, setSubjectId] = useState('');
  // 업로드 직후처럼 같은 필터로 다시 받아와야 할 때 값을 올려 이펙트를 다시 돌린다.
  const [reloadKey, setReloadKey] = useState(0);
  const [subjects, setSubjects] = useState([]);

  // 어떤 조건으로 받은 결과인지 함께 담아 두면, 조건이 바뀐 직후를 렌더 시점에
  // "로딩 중"으로 판별할 수 있어 이펙트에서 상태를 초기화할 필요가 없다.
  const requestKey = `${reloadKey}:${subjectId}`;
  const [result, setResult] = useState({ requestKey: null, materials: [], error: '' });

  useEffect(() => {
    let cancelled = false;
    findMyMaterials(subjectId)
      .then((materials) => {
        if (!cancelled) setResult({ requestKey, materials, error: '' });
      })
      .catch((err) => {
        if (!cancelled) setResult({ requestKey, materials: [], error: err.message });
      });
    return () => {
      cancelled = true;
    };
  }, [subjectId, requestKey]);

  // 과목 필터 목록은 필터가 바뀌어도 다시 받을 필요가 없다.
  useEffect(() => {
    findAllSubjects().then(setSubjects).catch(() => setSubjects([]));
  }, []);

  const loading = result.requestKey !== requestKey;
  const { materials, error } = loading ? { materials: [], error: '' } : result;

  const [uploadOpen, setUploadOpen] = useState(false);

  const handleUploaded = () => {
    setUploadOpen(false);
    setReloadKey((prev) => prev + 1);
  };

  return (
    <>
      <section className={styles.hero}>
        <div className={styles.heroInner}>
          <div>
            <h1 className={styles.heroTitle}>오늘 배운 내용을 정리해보세요</h1>
            <p className={styles.heroSubtitle}>PDF를 올리면 AI가 TIL 초안을 만들어드려요</p>
          </div>
          <Button onClick={() => setUploadOpen(true)}>자료 업로드</Button>
        </div>
      </section>

      <div className="page">
        <div className={styles.filters}>
          <span className="section-title" style={{ marginBottom: 0 }}>
            학습자료
          </span>
          <select
            className={styles.filterSelect}
            value={subjectId}
            onChange={(event) => setSubjectId(event.target.value)}
            aria-label="과목 필터"
          >
            <option value="">전체 과목</option>
            {subjects.map((subject) => (
              <option key={subject.id} value={subject.id}>
                {subject.name}
              </option>
            ))}
          </select>
        </div>

        {error && <p className={styles.error}>{error}</p>}

        {loading ? (
          <p className={styles.empty}>불러오는 중…</p>
        ) : materials.length === 0 ? (
          <p className={styles.empty}>아직 업로드한 학습자료가 없습니다.</p>
        ) : (
          <div className={styles.grid}>
            {materials.map((material) => (
              <Link
                key={material.id}
                to={`/materials/${material.id}`}
                className={styles.card}
              >
                <div className={styles.cardHead}>
                  <span className={styles.cardTitle}>{material.title}</span>
                  <StatusBadge status={material.processingStatus} />
                </div>
                <p className={styles.cardDate}>{formatDate(material.uploadedAt)}</p>
                <div className={styles.cardTags}>
                  <Tag>{material.subject.name}</Tag>
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>

      {uploadOpen && (
        <MaterialUploadDialog
          onClose={() => setUploadOpen(false)}
          onUploaded={handleUploaded}
        />
      )}
    </>
  );
}
