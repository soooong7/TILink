import { useEffect, useState } from 'react';
import { findAllSubjects } from '../api/subjects';
import { uploadMaterial } from '../api/materials';
import Button from './ui/Button';
import Field from './ui/Field';
import { controlClass, fileControlClass } from './ui/fieldStyles';
import styles from './MaterialUploadDialog.module.css';

/** 학습자료 업로드 모달. 업로드에 성공하면 생성된 자료를 onUploaded 로 넘긴다. */
export default function MaterialUploadDialog({ onClose, onUploaded }) {
  const [subjects, setSubjects] = useState([]);
  const [subjectId, setSubjectId] = useState('');
  const [title, setTitle] = useState('');
  const [file, setFile] = useState(null);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    findAllSubjects()
      .then((list) => {
        setSubjects(list);
        setSubjectId((prev) => prev || list[0]?.id || '');
      })
      .catch((err) => setError(err.message));
  }, []);

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!file) {
      setError('PDF 파일을 선택해 주세요.');
      return;
    }
    setError('');
    setSubmitting(true);
    try {
      const material = await uploadMaterial({ subjectId, title: title.trim(), file });
      onUploaded(material);
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className={styles.backdrop} onClick={onClose}>
      {/* 모달 안쪽 클릭으로 닫히지 않게 이벤트를 막는다. */}
      <div className={styles.dialog} onClick={(event) => event.stopPropagation()}>
        <h2 className={styles.title}>학습자료 업로드</h2>

        <form className={styles.form} onSubmit={handleSubmit}>
          {error && <p className={styles.error}>{error}</p>}

          <Field label="과목" htmlFor="subjectId">
            <select
              id="subjectId"
              className={controlClass}
              value={subjectId}
              onChange={(event) => setSubjectId(event.target.value)}
              required
            >
              {subjects.map((subject) => (
                <option key={subject.id} value={subject.id}>
                  {subject.name}
                </option>
              ))}
            </select>
          </Field>

          <div>
            <Field
              label="제목 (선택)"
              name="title"
              type="text"
              maxLength={255}
              value={title}
              onChange={(event) => setTitle(event.target.value)}
            />
            <p className={styles.hint}>비워두면 파일명이 제목으로 쓰입니다.</p>
          </div>

          <Field label="PDF 파일" htmlFor="file">
            <input
              id="file"
              className={fileControlClass}
              type="file"
              accept="application/pdf"
              onChange={(event) => setFile(event.target.files?.[0] ?? null)}
              required
            />
          </Field>

          <div className={styles.actions}>
            <Button variant="secondary" onClick={onClose} disabled={submitting}>
              취소
            </Button>
            <Button type="submit" disabled={submitting || !subjectId}>
              {submitting ? '업로드 중…' : '업로드'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
