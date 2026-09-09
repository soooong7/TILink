import styles from './Tag.module.css';

// 과목·기술 태그 표시용 칩.
export default function Tag({ children }) {
  return <span className={styles.tag}>{children}</span>;
}
