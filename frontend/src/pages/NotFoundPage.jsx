import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div className="page">
      <h1>페이지를 찾을 수 없습니다</h1>
      <p style={{ marginTop: 12 }}>
        <Link to="/materials">학습자료 목록으로 가기</Link>
      </p>
    </div>
  );
}
