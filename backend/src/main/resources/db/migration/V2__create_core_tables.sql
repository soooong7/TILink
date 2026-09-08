-- TILink 핵심 테이블 8개 생성
-- 외래키(FK)가 참조하는 테이블이 먼저 존재해야 하므로 아래 순서를 지킨다.
--   1) 독립 테이블: admins, users, subjects, tags
--   2) materials       -> users, subjects 참조
--   3) material_chunks -> materials 참조
--   4) tils            -> users, materials 참조
--   5) til_tags        -> tils, tags 참조
--
-- PK는 ERD 정의에 따라 varchar(36)이며, 애플리케이션에서 UUID 문자열을 생성해 넣는다.

-- ==========================================
-- 1) 독립 테이블
-- ==========================================

-- 서비스 관리자 계정
CREATE TABLE admins (
    id         varchar(36)  PRIMARY KEY,
    email      varchar(255) NOT NULL UNIQUE,
    password   varchar(255) NOT NULL,
    name       varchar(100) NOT NULL,
    created_at timestamp    NOT NULL DEFAULT now()
);

-- 교육생 계정
CREATE TABLE users (
    id         varchar(36)  PRIMARY KEY,
    email      varchar(255) NOT NULL UNIQUE,
    password   varchar(255) NOT NULL,
    name       varchar(100) NOT NULL,
    created_at timestamp    NOT NULL DEFAULT now()
);

-- 과목/기술 카테고리 (관리자가 관리하는 공통 데이터)
CREATE TABLE subjects (
    id         varchar(36)  PRIMARY KEY,
    name       varchar(100) NOT NULL UNIQUE,
    created_at timestamp    NOT NULL DEFAULT now()
);

-- 기술/주제 태그
CREATE TABLE tags (
    id   varchar(36)  PRIMARY KEY,
    name varchar(100) NOT NULL UNIQUE
);

-- ==========================================
-- 2) 학습자료
-- ==========================================

CREATE TABLE materials (
    id                varchar(36)  PRIMARY KEY,
    user_id           varchar(36)  NOT NULL,
    subject_id        varchar(36)  NOT NULL,
    title             varchar(255) NOT NULL,
    file_url          varchar(512) NOT NULL,
    -- FastAPI 비동기 처리 상태: UPLOADED / PROCESSING / DONE
    processing_status varchar(20)  NOT NULL DEFAULT 'UPLOADED',
    uploaded_at       timestamp    NOT NULL DEFAULT now(),

    -- 사용자가 삭제되면 그 사용자의 학습자료도 함께 삭제된다.
    CONSTRAINT fk_materials_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    -- 과목은 공통 데이터이므로, 사용 중인 과목은 삭제되지 않도록 막는다.
    CONSTRAINT fk_materials_subject
        FOREIGN KEY (subject_id) REFERENCES subjects (id) ON DELETE RESTRICT,
    -- 오타/잘못된 상태값이 저장되는 것을 DB 차원에서 막는다.
    CONSTRAINT ck_materials_processing_status
        CHECK (processing_status IN ('UPLOADED', 'PROCESSING', 'DONE'))
);

-- ==========================================
-- 3) 학습자료 청크 (RAG 검색 대상)
-- ==========================================

CREATE TABLE material_chunks (
    id          varchar(36)  PRIMARY KEY,
    material_id varchar(36)  NOT NULL,
    chunk_index int          NOT NULL,
    content     text         NOT NULL,
    -- text-embedding-3-small 기준 1536차원 벡터
    embedding   vector(1536) NOT NULL,
    created_at  timestamp    NOT NULL DEFAULT now(),

    CONSTRAINT fk_material_chunks_material
        FOREIGN KEY (material_id) REFERENCES materials (id) ON DELETE CASCADE,
    -- 재처리 등으로 같은 자료의 같은 순번 청크가 중복 저장되는 사고를 막는다.
    CONSTRAINT uq_material_chunks_material_index
        UNIQUE (material_id, chunk_index)
);

-- ==========================================
-- 4) TIL
-- ==========================================

CREATE TABLE tils (
    id               varchar(36)  PRIMARY KEY,
    user_id          varchar(36)  NOT NULL,
    material_id      varchar(36)  NOT NULL,
    title            varchar(255) NOT NULL,
    content          text         NOT NULL,
    -- TIL 저장 직후 임베딩 생성 전일 수 있으므로 NULL 허용
    embedding        vector(1536),
    -- 최종 열람 일시. 복습 알림의 기준값이며, 한 번도 열람하지 않았으면 NULL
    last_reviewed_at timestamp,
    created_at       timestamp    NOT NULL DEFAULT now(),
    updated_at       timestamp    NOT NULL DEFAULT now(),

    CONSTRAINT fk_tils_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_tils_material
        FOREIGN KEY (material_id) REFERENCES materials (id) ON DELETE CASCADE
);

-- ==========================================
-- 5) TIL-태그 연결 (N:M)
-- ==========================================

CREATE TABLE til_tags (
    til_id varchar(36) NOT NULL,
    tag_id varchar(36) NOT NULL,

    -- 같은 TIL에 같은 태그가 두 번 붙는 것을 복합 PK로 막는다.
    CONSTRAINT pk_til_tags PRIMARY KEY (til_id, tag_id),
    CONSTRAINT fk_til_tags_til
        FOREIGN KEY (til_id) REFERENCES tils (id) ON DELETE CASCADE,
    CONSTRAINT fk_til_tags_tag
        FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE
);
