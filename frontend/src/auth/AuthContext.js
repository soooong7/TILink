import { createContext } from 'react';

// Provider 와 분리해 두면 컴포넌트 파일이 컴포넌트만 내보내 Fast Refresh 가 유지된다.
export const AuthContext = createContext(null);
