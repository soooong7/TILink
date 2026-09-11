-- AI 분석 실패 상태(FAILED)를 허용한다.
--
-- V2 의 CHECK 제약은 UPLOADED / PROCESSING / DONE 세 가지만 허용해서,
-- FastAPI 가 처리에 실패했을 때 그 사실을 기록할 방법이 없었다.
-- (실패해도 PROCESSING 으로 남으면 "처리 중"과 "실패"를 구분할 수 없다)
--
-- CHECK 제약은 ALTER 로 수정할 수 없어 지웠다가 다시 만든다.

ALTER TABLE materials DROP CONSTRAINT ck_materials_processing_status;

ALTER TABLE materials ADD CONSTRAINT ck_materials_processing_status
    CHECK (processing_status IN ('UPLOADED', 'PROCESSING', 'DONE', 'FAILED'));
