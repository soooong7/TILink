import axios from 'axios';
import { clearToken, getToken } from '../auth/tokenStorage';

// 기본값은 '/api'. dev 에서는 Vite 프록시가 8090 백엔드로 넘긴다.
const baseURL = import.meta.env.VITE_API_BASE_URL || '/api';

export const client = axios.create({ baseURL });

// 토큰 만료로 401 을 받았을 때 화면을 로그인으로 되돌리기 위한 콜백.
// api 계층이 react-router 에 의존하지 않도록 AuthProvider 가 주입한다.
let unauthorizedHandler = null;

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler;
}

// 인증이 필요한 요청에 Authorization 헤더를 자동으로 붙인다.
client.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 로그인 실패(INVALID_CREDENTIALS)도 401 이라, 상태 코드만 보고 로그아웃 처리하면
// 로그인 화면에서 비밀번호를 틀릴 때마다 세션을 건드리게 된다.
// 토큰을 실제로 붙여 보낸 요청이 거절된 경우에만 만료로 본다.
function isExpiredSession(error) {
  return error.response?.status === 401 && Boolean(error.config?.headers?.Authorization);
}

/**
 * 백엔드 에러 응답({code, message})과 네트워크 실패를 같은 모양으로 맞춘다.
 * 화면에서는 err.code 로 분기하고 err.message 를 그대로 보여주면 된다.
 */
export class ApiError extends Error {
  constructor(code, message, status) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.status = status;
  }
}

client.interceptors.response.use(
  (response) => response,
  (error) => {
    const { response } = error;

    if (!response) {
      return Promise.reject(
        new ApiError('NETWORK_ERROR', '서버에 연결할 수 없습니다.', 0),
      );
    }

    // 리프레시 토큰이 없는 구조라 만료는 곧 재로그인이다. 토큰을 지우고 화면을 되돌린다.
    if (isExpiredSession(error)) {
      clearToken();
      unauthorizedHandler?.();
    }

    const data = response.data ?? {};
    return Promise.reject(
      new ApiError(
        data.code ?? 'INTERNAL_ERROR',
        data.message ?? '서버 오류가 발생했습니다.',
        response.status,
      ),
    );
  },
);
