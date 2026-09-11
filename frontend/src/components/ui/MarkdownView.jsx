import Markdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import styles from './MarkdownView.module.css';

/**
 * 마크다운 미리보기.
 *
 * remark-gfm 을 쓰는 이유는 표와 체크리스트 때문이다. 교안 정리에는 비교표가 자주
 * 나오는데 기본 마크다운 문법에는 표가 없다.
 */
export default function MarkdownView({ markdown }) {
  return (
    <div className={styles.body}>
      <Markdown remarkPlugins={[remarkGfm]}>{markdown}</Markdown>
    </div>
  );
}
