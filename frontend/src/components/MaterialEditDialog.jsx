import { useEffect, useState } from 'react';
import { updateMaterial } from '../api/materials';
import { findAllSubjects } from '../api/subjects';
import Button from './ui/Button';
import Field from './ui/Field';
import { controlClass } from './ui/fieldStyles';
import styles from './MaterialUploadDialog.module.css';

/**
 * 학습자료 수정 모달. 제목과 과목만 바꾼다.
 *
 * PDF 교체가 없는 이유는 파일이 바뀌면 이미 만들어 둔 청크·임베딩이 모두 무의미해져
 * 재분석이 필요하고, 그건 새로 업로드하는 것과 사실상 같은 흐름이기 때문이다.
 *
 * 모달 뼈대는 업로드 모달과 같은 스타일시트를 공유한다.
 */
export default function MaterialEditDialog({ material, onClose, onUpdated }) {
  const [subjects, setSubjects] = useState([]);
  const [subjectId, setSubjectId] = useState(material.subject.id);
  const [title, setTitle] = useState(material.title);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    findAllSubjects().then(setSubjects).catch((err) => setError(err.message));
  }, []);

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!title.trim()) {
      setError('제목을 입력해 주세요.');
      return;
    }
    setError('');
    setSubmitting(true);
    try {
      onUpdated(await updateMaterial(material.id, { title: title.trim(), subjectId }));
    } catch (err) {
      setError(err.message);
      setSubmitting(false);
    }
  };

  return (
    <div className={styles.backdrop} onClick={onClose}>
      <div className={styles.dialog} onClick={(event) => event.stopPropagation()}>
        <h2 className={styles.title}>학습자료 수정</h2>

        <form className={styles.form} onSubmit={handleSubmit}>
          {error && <p className={styles.error}>{error}</p>}

          <Field
            label="제목"
            name="title"
            type="text"
            maxLength={255}
            value={title}
            onChange={(event) => setTitle(event.target.value)}
          />

          <Field label="과목" htmlFor="subjectId">
            <select
              id="subjectId"
              className={controlClass}
              value={subjectId}
              onChange={(event) => setSubjectId(event.target.value)}
            >
              {/* 과목 목록을 불러오기 전에도 현재 과목은 보여야 한다. */}
              {(subjects.length > 0 ? subjects : [material.subject]).map((subject) => (
                <option key={subject.id} value={subject.id}>
                  {subject.name}
                </option>
              ))}
            </select>
          </Field>

          <p className={styles.hint}>
            PDF 파일은 바꿀 수 없습니다. 다른 파일로 바꾸려면 새로 업로드해 주세요.
          </p>

          <div className={styles.actions}>
            <Button variant="secondary" onClick={onClose} disabled={submitting}>
              취소
            </Button>
            <Button type="submit" disabled={submitting}>
              {submitting ? '저장 중…' : '저장'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
