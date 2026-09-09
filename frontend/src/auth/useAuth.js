import { useContext } from 'react';
import { AuthContext } from './AuthContext';

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth 는 AuthProvider 안에서만 쓸 수 있습니다.');
  }
  return context;
}
