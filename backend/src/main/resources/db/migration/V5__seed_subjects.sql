-- 과목/기술 카테고리 기본 데이터
--
-- subjects 는 관리자가 관리하는 공통 데이터이고 사용자는 이 중에서 고르기만 한다.
-- 아직 관리자 API 가 없어 이 테이블이 비어 있으면 학습자료 업로드 자체가 불가능하므로,
-- 마이그레이션으로 기본 과목을 넣어 둔다. (관리자 등록/수정 API 는 관리자 API 단계에서 추가)
--
-- ON CONFLICT DO NOTHING: 같은 이름의 과목이 이미 있으면 조용히 건너뛴다.
-- gen_random_uuid(): PostgreSQL 13+ 내장 함수. 애플리케이션의 @GeneratedValue(UUID) 와 같은 형식이다.
INSERT INTO subjects (id, name)
VALUES (gen_random_uuid()::varchar, 'AI엔지니어링'),
       (gen_random_uuid()::varchar, '백엔드'),
       (gen_random_uuid()::varchar, '프론트엔드'),
       (gen_random_uuid()::varchar, '인프라'),
       (gen_random_uuid()::varchar, '데이터분석')
ON CONFLICT (name) DO NOTHING;
