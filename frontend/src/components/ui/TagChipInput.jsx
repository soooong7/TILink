import { useState } from 'react';
import styles from './TagChipInput.module.css';

/**
 * 삭제 가능한 태그 칩 + 직접 추가 입력.
 *
 * 값(배열)은 부모가 들고 있고 이 컴포넌트는 변경만 알린다(제어 컴포넌트).
 * 저장 시 부모가 그대로 API 에 넘길 수 있어야 하기 때문이다.
 */
export default function TagChipInput({ tags, onChange }) {
  const [draft, setDraft] = useState('');

  const addTag = () => {
    const name = draft.trim();
    // 같은 태그를 두 번 넣으면 서버에서도 중복 연결이 되지 않으므로 화면에서 먼저 막는다.
    if (name && !tags.includes(name)) {
      onChange([...tags, name]);
    }
    setDraft('');
  };

  const handleKeyDown = (event) => {
    // Enter 로 추가한다. 폼 안에 있으므로 기본 동작(제출)을 막아야 한다.
    if (event.key === 'Enter') {
      event.preventDefault();
      addTag();
      return;
    }
    // 입력이 비어 있을 때 Backspace 를 누르면 마지막 태그를 지운다 (칩 입력의 관례).
    if (event.key === 'Backspace' && !draft && tags.length > 0) {
      onChange(tags.slice(0, -1));
    }
  };

  return (
    <div className={styles.wrapper}>
      {tags.map((tag) => (
        <span key={tag} className={styles.chip}>
          {tag}
          <button
            type="button"
            className={styles.remove}
            onClick={() => onChange(tags.filter((item) => item !== tag))}
            aria-label={`${tag} 태그 삭제`}
          >
            ×
          </button>
        </span>
      ))}

      <input
        className={styles.input}
        value={draft}
        onChange={(event) => setDraft(event.target.value)}
        onKeyDown={handleKeyDown}
        onBlur={addTag}
        placeholder={tags.length === 0 ? '태그를 입력하고 Enter' : '추가'}
        aria-label="태그 추가"
      />
    </div>
  );
}
