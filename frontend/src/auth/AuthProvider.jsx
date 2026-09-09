import { useCallback, useEffect, useMemo, useState } from 'react';
import { getMe, login as loginRequest } from '../api/auth';
import { setUnauthorizedHandler } from '../api/client';
import { AuthContext } from './AuthContext';
import { clearToken, getToken, setToken } from './tokenStorage';

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  // 새로고침 직후에는 토큰이 아직 유효한지 모른다. 확인이 끝나기 전에 화면을
  // 그리면 로그인 상태인데도 로그인 페이지가 잠깐 보인다.
  const [initializing, setInitializing] = useState(Boolean(getToken()));

  const logout = useCallback(() => {
    clearToken();
    setUser(null);
  }, []);

  // 401 을 받으면 토큰은 client 인터셉터가 이미 지웠고, 여기서는 화면 상태만 비운다.
  useEffect(() => {
    setUnauthorizedHandler(() => setUser(null));
    return () => setUnauthorizedHandler(null);
  }, []);

  // 저장된 토큰이 있으면 /users/me 로 실제 유효한지 확인한다.
  useEffect(() => {
    if (!getToken()) {
      return;
    }
    getMe()
      .then(setUser)
      .catch(() => clearToken())
      .finally(() => setInitializing(false));
  }, []);

  const login = useCallback(async (credentials) => {
    const { accessToken } = await loginRequest(credentials);
    setToken(accessToken);
    // 토큰만으로는 이름·이메일을 알 수 없어 프로필을 한 번 더 받아온다.
    const me = await getMe();
    setUser(me);
    return me;
  }, []);

  const value = useMemo(
    () => ({ user, isAuthenticated: Boolean(user), initializing, login, logout }),
    [user, initializing, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
