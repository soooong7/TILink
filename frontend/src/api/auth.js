import { client } from './client';

// POST /api/auth/login -> { accessToken, tokenType, expiresIn }
export async function login({ email, password }) {
  const { data } = await client.post('/auth/login', { email, password });
  return data;
}

// POST /api/users/signup -> 201 { id, email, name, createdAt }
export async function signup({ email, password, name }) {
  const { data } = await client.post('/users/signup', { email, password, name });
  return data;
}

// GET /api/users/me -> { id, email, name, createdAt }
export async function getMe() {
  const { data } = await client.get('/users/me');
  return data;
}
