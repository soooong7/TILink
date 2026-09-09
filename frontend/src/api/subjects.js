import { client } from './client';

// GET /api/subjects -> [{ id, name }] (인증 필요)
export async function findAllSubjects() {
  const { data } = await client.get('/subjects');
  return data;
}
