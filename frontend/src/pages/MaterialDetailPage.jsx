import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { deleteMaterial, findMaterial } from '../api/materials';
import MaterialEditDialog from '../components/MaterialEditDialog';
import Button from '../components/ui/Button';
import ConfirmDialog from '../components/ui/ConfirmDialog';
import StatusBadge from '../components/ui/StatusBadge';
import Tag from '../components/ui/Tag';
import { formatDateTime } from '../utils/format';
import styles from './MaterialDetailPage.module.css';

/**
 * 처리 상태별로 TIL 생성이 불가능한 이유. DONE 이 아니면 버튼을 막고 이 문구를 보여준다.
 * AI 초안은 material_chunks 를 컨텍스트로 쓰기 때문에 분석이 끝나야만 만들 수 있다.
 */
const DRAFT_BLOCKED_REASON = {
  UPLOADED: '자료 분석이 아직 시작되지 않았습니다. 잠시 후 다시 확인해 주세요.',
  PROCESSING: '자료를 분석하는 중입니다. 완료되면 TIL 을 만들 수 있습니다.',
  FAILED: '자료 분석에 실패해 TIL 을 만들 수 없습니다. 파일을 다시 업로드해 주세요.',
};

/**
 * 삭제 확인 문구. 자료를 지우면 그 자료로 만든 TIL 도 함께 사라지므로(DB FK 가 CASCADE),
 * 몇 개가 사라지는지 숫자로 알린다. "정말 삭제할까요?" 만으로는 판단할 근거가 없다.
 */
function deleteDescription(material) {
  const base = `"${material.title}" 과 업로드된 PDF 가 삭제됩니다. 되돌릴 수 없습니다.`;
  if (material.tilCount === 0) {
    return base;
  }
  return `${base}\n이 자료로 작성한 TIL ${material.tilCount}개도 함께 삭제됩니다.`;
}

export default function MaterialDetailPage() {
  const { materialId } = useParams();
  const navigate = useNavigate();
  // 어떤 ID 의 결과인지 함께 담아둔다. 그래야 URL 이 바뀐 직후를 "로딩 중"으로
  // 렌더 시점에 판별할 수 있고, 이펙트에서 상태를 초기화하지 않아도 된다.
  const [result, setResult] = useState({ materialId: null, material: null, error: '' });
  // 'edit' | 'delete' | null. 한 번에 하나만 뜨므로 boolean 두 개 대신 한 값으로 둔다.
  const [dialog, setDialog] = useState(null);

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

  const handleDelete = async () => {
    await deleteMaterial(materialId);
    // 방금 지운 자료의 상세로 되돌아오지 않도록 히스토리에서 이 화면을 치운다.
    navigate('/materials', { replace: true });
  };

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
            <div className={styles.row}>
              <span className={styles.label}>작성한 TIL</span>
              <span className={styles.value}>{material.tilCount}개</span>
            </div>
          </div>

          <div className={styles.actions}>
            <Button
              disabled={material.processingStatus !== 'DONE'}
              onClick={() => navigate(`/materials/${materialId}/til-draft`)}
            >
              AI로 TIL 만들기
            </Button>
            <Button variant="secondary" onClick={() => setDialog('edit')}>
              수정
            </Button>
            <Button variant="secondary" onClick={() => setDialog('delete')}>
              삭제
            </Button>
            {DRAFT_BLOCKED_REASON[material.processingStatus] && (
              <p className={styles.reason}>{DRAFT_BLOCKED_REASON[material.processingStatus]}</p>
            )}
          </div>

          {/* 파일 다운로드 API 는 아직 백엔드에 없다. 있는 척하지 않는다. */}
          <p className={styles.notice}>원본 파일 다운로드는 다음 단계에서 연결됩니다.</p>

          {dialog === 'edit' && (
            <MaterialEditDialog
              material={material}
              onClose={() => setDialog(null)}
              onUpdated={(updated) => {
                setResult({ materialId, material: updated, error: '' });
                setDialog(null);
              }}
            />
          )}

          {dialog === 'delete' && (
            <ConfirmDialog
              title="이 학습자료를 삭제할까요?"
              description={deleteDescription(material)}
              onConfirm={handleDelete}
              onClose={() => setDialog(null)}
            />
          )}
        </>
      )}
    </div>
  );
}
