import { Navigate, Route, Routes } from 'react-router-dom';
import RequireAuth from './auth/RequireAuth';
import AppLayout from './components/layout/AppLayout';
import LoginPage from './pages/LoginPage';
import MaterialDetailPage from './pages/MaterialDetailPage';
import MaterialListPage from './pages/MaterialListPage';
import NotFoundPage from './pages/NotFoundPage';
import SignupPage from './pages/SignupPage';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/materials" replace />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />

      {/* 아래는 모두 로그인이 필요하다. 백엔드도 /api/subjects 부터 인증을 요구한다. */}
      <Route element={<RequireAuth />}>
        <Route element={<AppLayout />}>
          <Route path="/materials" element={<MaterialListPage />} />
          <Route path="/materials/:materialId" element={<MaterialDetailPage />} />
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
