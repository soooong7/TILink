import styles from './Field.module.css';

// select·input[type=file] 처럼 Field 안에서 직접 렌더하는 컨트롤에 붙이는 공통 클래스.
// 컴포넌트 파일과 분리해야 Fast Refresh 가 깨지지 않는다.
export const controlClass = styles.control;
export const fileControlClass = `${styles.control} ${styles.file}`;
