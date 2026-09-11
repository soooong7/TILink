import { useState } from 'react';
import styles from './MarkdownSource.module.css';

/**
 * 마크다운 원문 + 복사 버튼.
 *
 * 블로그(velog·티스토리)에 그대로 붙여넣는 것이 이 화면의 목적이라, 렌더 결과가 아니라
 * 원문을 보여주고 클립보드로 복사시킨다.
 */
export default function MarkdownSource({ markdown, fileName = 'TIL.md' }) {
  const [copied, setCopied] = useState(false);

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(markdown);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // http 환경이나 권한 거부로 클립보드 API 가 막힐 수 있다. 이때는 직접 선택해 복사하도록 안내한다.
      setCopied(false);
      window.alert('복사에 실패했습니다. 아래 내용을 직접 선택해 복사해 주세요.');
    }
  };

  return (
    <div className={styles.wrapper}>
      <div className={styles.head}>
        <span className={styles.file}>{fileName}</span>
        <span className={styles.lines}>· {markdown.split('\n').length} lines</span>
        <button type="button" className={styles.copy} onClick={copy}>
          {copied ? '복사됨' : '복사하기'}
        </button>
      </div>
      <pre className={styles.code}>{markdown}</pre>
    </div>
  );
}
