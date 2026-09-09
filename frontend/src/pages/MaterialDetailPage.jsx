import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { findMaterial } from '../api/materials';
import StatusBadge from '../components/ui/StatusBadge';
import Tag from '../components/ui/Tag';
import { formatDateTime } from '../utils/format';
import styles from './MaterialDetailPage.module.css';

export default function MaterialDetailPage() {
  const { materialId } = useParams();
  // 어떤 ID 의 결과인지 함께 담아둔다. 그래야 URL 이 바뀐 직후를 "로딩 중"으로
  // 렌더 시점에 판별할 수 있고, 이펙트에서 상태를 초기화하지 않아도 된다.
  const [result, setResult] = useState({ materialId: null, material: null, error: '' });

  useEffect(() => {
    let cancelled = false;
    findMaterial(materialId)
      .then((material) => {
        if (!cancelled) setResult({ materialId, material, error: '' });
      })
      .catch((err) => {
        if (!cancelled) setResult({ materialId, material: null, error: err.message });
      });
    // 요청이 끝나기 전에 다른 자료로 이동하면 늦게 온 응답을 버린다.
    return () => {
      cancelled = true;
    };
  }, [materialId]);

  const loading = result.materialId !== materialId;
  const { material, error } = loading ? { material: null, error: '' } : result;

  return (
    <div className="page">
      <Link to="/materials" className={styles.back}>
        ← 학습자료 목록
      </Link>

      {error && <p className={styles.error}>{error}</p>}

      {loading && <p>불러오는 중…</p>}

      {material && (
        <>
          <div className={styles.head}>
            <h1 className={styles.title}>{material.title}</h1>
            <StatusBadge status={material.processingStatus} />
          </div>

          <div className={styles.meta}>
            <div className={styles.row}>
              <span className={styles.label}>과목</span>
              <span className={styles.value}>
                <Tag>{material.subject.name}</Tag>
              </span>
            </div>
            <div className={styles.row}>
              <span className={styles.label}>원본 파일명</span>
              <span className={styles.value}>{material.originalFileName}</span>
            </div>
            <div className={styles.row}>
              <span className={styles.label}>저장 키</span>
              <span className={styles.value}>{material.fileUrl}</span>
            </div>
            <div className={styles.row}>
              <span className={styles.label}>업로드 일시</span>
              <span className={styles.value}>{formatDateTime(material.uploadedAt)}</span>
            </div>
          </div>

          {/* 파일 다운로드 API 와 TIL 생성은 아직 백엔드에 없다. 있는 척하지 않는다. */}
          <p className={styles.notice}>
            AI TIL 초안 생성과 원본 파일 다운로드는 다음 단계에서 연결됩니다.
          </p>
        </>
      )}
    </div>
  );
}
