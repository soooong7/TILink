// 토큰을 읽고 쓰는 유일한 지점. 저장 위치(localStorage)를 바꿀 때 여기만 고치면 된다.
const TOKEN_KEY = 'tilink.accessToken';

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}
