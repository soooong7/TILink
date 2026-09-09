import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { signup } from '../api/auth';
import { useAuth } from '../auth/useAuth';
import Button from '../components/ui/Button';
import Field from '../components/ui/Field';
import styles from './AuthPage.module.css';

export default function SignupPage() {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState({ email: '', password: '', name: '' });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await signup(form);
      // 가입 직후 다시 로그인을 시키면 방금 입력한 값을 또 받아야 한다. 바로 로그인시킨다.
      await login({ email: form.email, password: form.password });
      navigate('/materials', { replace: true });
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className={styles.wrap}>
      <div className={styles.card}>
        <h1 className={styles.brand}>회원가입</h1>
        <p className={styles.subtitle}>TILink에서 학습 기록을 이어보세요</p>

        <form className={styles.form} onSubmit={handleSubmit}>
          {error && <p className={styles.error}>{error}</p>}

          <Field
            label="이름"
            name="name"
            type="text"
            autoComplete="name"
            maxLength={100}
            value={form.name}
            onChange={handleChange}
            required
          />
          <Field
            label="이메일"
            name="email"
            type="email"
            autoComplete="email"
            maxLength={255}
            value={form.email}
            onChange={handleChange}
            required
          />
          {/* 백엔드 SignupRequest 가 8~64자를 요구한다. 서버까지 가기 전에 걸러준다. */}
          <Field
            label="비밀번호 (8자 이상)"
            name="password"
            type="password"
            autoComplete="new-password"
            minLength={8}
            maxLength={64}
            value={form.password}
            onChange={handleChange}
            required
          />

          <Button type="submit" block disabled={submitting}>
            {submitting ? '가입 중…' : '가입하기'}
          </Button>
        </form>

        <p className={styles.footer}>
          이미 계정이 있으신가요? <Link to="/login">로그인</Link>
        </p>
      </div>
    </div>
  );
}
