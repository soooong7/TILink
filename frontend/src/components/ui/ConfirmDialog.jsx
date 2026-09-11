import { useState } from 'react';
import Button from './Button';
import styles from './ConfirmDialog.module.css';

/**
 * 되돌릴 수 없는 동작을 확인받는 모달.
 *
 * window.confirm 대신 쓰는 이유는 두 가지다. 삭제로 함께 사라지는 것들을 여러 줄로
 * 설명할 수 있고, 서버 요청이 실패했을 때 그 이유를 같은 자리에서 보여줄 수 있다.
 *
 * @param onConfirm 확인을 눌렀을 때 실행할 async 함수. 던진 에러는 모달 안에 표시된다.
 */
export default function ConfirmDialog({
  title,
  description,
  confirmLabel = '삭제',
  onConfirm,
  onClose,
}) {
  const [error, setError] = useState('');
  const [running, setRunning] = useState(false);

  const handleConfirm = async () => {
    setError('');
    setRunning(true);
    try {
      await onConfirm();
    } catch (err) {
      setError(err.message);
      // 실패했으면 모달을 닫지 않는다. 닫아 버리면 사용자가 이유를 볼 자리가 없다.
      setRunning(false);
    }
  };

  return (
    <div className={styles.backdrop} onClick={running ? undefined : onClose}>
      <div className={styles.dialog} onClick={(event) => event.stopPropagation()}>
        <h2 className={styles.title}>{title}</h2>
        {description && <p className={styles.description}>{description}</p>}
        {error && <p className={styles.error}>{error}</p>}

        <div className={styles.actions}>
          <Button variant="secondary" onClick={onClose} disabled={running}>
            취소
          </Button>
          <Button variant="danger" onClick={handleConfirm} disabled={running}>
            {running ? '처리 중…' : confirmLabel}
          </Button>
        </div>
      </div>
    </div>
  );
}
