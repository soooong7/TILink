-- materials 에 원본 파일명 컬럼 추가
--
-- 디스크에는 사용자가 올린 파일명을 그대로 쓰지 않는다.
-- (경로 탈출(../), 같은 이름 덮어쓰기, OS 별 인코딩 문제를 한 번에 피하려고 UUID 로 저장한다)
-- 대신 화면에 "무슨 파일을 올렸는지" 보여줄 원본 이름을 별도 컬럼에 남긴다.
--
-- DEFAULT '' 를 잠깐 붙였다 떼는 이유: 이미 들어있는 행이 있어도 NOT NULL 을 걸 수 있게 하기 위함이며,
-- 이후 INSERT 는 항상 값을 명시하도록 기본값을 제거한다.
ALTER TABLE materials
    ADD COLUMN original_file_name varchar(255) NOT NULL DEFAULT '';

ALTER TABLE materials
    ALTER COLUMN original_file_name DROP DEFAULT;
