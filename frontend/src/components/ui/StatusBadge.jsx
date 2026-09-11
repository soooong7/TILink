import styles from './StatusBadge.module.css';

// 백엔드 ProcessingStatus enum 값을 화면 문구로 옮긴다.
const STATUS = {
  UPLOADED: { label: '업로드됨', className: styles.uploaded },
  PROCESSING: { label: '분석중', className: styles.processing },
  DONE: { label: '완료', className: styles.done },
  FAILED: { label: '분석 실패', className: styles.failed },
};

export default function StatusBadge({ status }) {
  const { label, className } = STATUS[status] ?? {
    label: status,
    className: styles.uploaded,
  };

  return <span className={`${styles.badge} ${className}`}>{label}</span>;
}
