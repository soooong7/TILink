-- pgvector 확장 활성화
-- vector(1536) 컬럼 타입과 코사인 유사도 연산자(<=>)를 사용하기 위해 반드시 먼저 실행되어야 한다.
-- 테이블 생성(V2)과 분리해 둔 이유: 이 단계가 실패하면 "DB에 pgvector가 없다"는 것을
-- 바로 알 수 있고, 원인 파악이 쉬워지기 때문이다.
CREATE EXTENSION IF NOT EXISTS vector;
