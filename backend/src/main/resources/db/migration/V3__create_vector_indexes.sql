-- 조회 성능을 위한 인덱스
-- 테이블 생성(V2)과 분리한 이유: 인덱스는 이후 성능 튜닝 대상이므로
-- (HNSW <-> IVFFlat 교체, 파라미터 조정 등) 별도 파일로 관리하는 편이 낫다.

-- ==========================================
-- 벡터 인덱스 (관련 학습 조회 = 코사인 유사도 검색)
-- ==========================================
-- HNSW: pgvector의 근사 최근접 이웃(ANN) 인덱스. 정확도와 속도의 균형이 좋다.
-- vector_cosine_ops: 코사인 거리(연산자 <=>) 기준으로 검색하겠다는 의미.
--   → 반드시 쿼리에서도 같은 연산자(<=>)를 써야 인덱스를 탄다.

CREATE INDEX idx_material_chunks_embedding_hnsw
    ON material_chunks USING hnsw (embedding vector_cosine_ops);

CREATE INDEX idx_tils_embedding_hnsw
    ON tils USING hnsw (embedding vector_cosine_ops);

-- ==========================================
-- 일반 인덱스 (외래키 및 자주 쓰는 조회 조건)
-- ==========================================
-- PostgreSQL은 외래키 컬럼에 인덱스를 자동으로 만들어 주지 않는다.
-- "내 학습자료 목록", "내 TIL 목록" 같은 조회가 매번 전체 스캔이 되지 않도록 직접 만든다.
-- (material_chunks.material_id 와 til_tags.til_id 는 각각
--  UNIQUE(material_id, chunk_index), PK(til_id, tag_id)의 선두 컬럼이라 이미 인덱스가 있다)

CREATE INDEX idx_materials_user_id ON materials (user_id);
CREATE INDEX idx_materials_subject_id ON materials (subject_id);
CREATE INDEX idx_tils_material_id ON tils (material_id);
CREATE INDEX idx_til_tags_tag_id ON til_tags (tag_id);

-- 복습 알림: "특정 사용자의 TIL 중 last_reviewed_at 이 오래된 것" 조회용 복합 인덱스
CREATE INDEX idx_tils_user_id_last_reviewed_at ON tils (user_id, last_reviewed_at);
