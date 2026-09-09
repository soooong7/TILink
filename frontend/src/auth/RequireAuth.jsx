import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from './useAuth';

// 로그인하지 않았으면 /login 으로 보내고, 원래 가려던 위치를 state 로 넘겨
// 로그인 후 그 화면으로 되돌린다.
export default function RequireAuth() {
  const { isAuthenticated, initializing } = useAuth();
  const location = useLocation();

  if (initializing) {
    return <div className="page">불러오는 중…</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return <Outlet />;
}
