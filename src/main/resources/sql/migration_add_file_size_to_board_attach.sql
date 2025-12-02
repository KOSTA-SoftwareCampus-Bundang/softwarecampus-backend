-- 게시판 첨부파일 테이블에 file_size 컬럼 추가
-- 작성일: 2025-12-03
-- 목적: 프론트엔드에서 파일 크기 정보를 표시하기 위해 file_size 컬럼 추가

-- 1. file_size 컬럼 추가 (NOT NULL, 기본값 0)
ALTER TABLE board_attach
ADD COLUMN file_size BIGINT NOT NULL DEFAULT 0
COMMENT '파일 크기 (bytes)';

-- 2. 기존 데이터에 대한 처리
-- 기존 데이터에는 파일 크기 정보가 없으므로 0으로 설정됨
-- 이후 새로 업로드되는 파일은 실제 크기가 저장됨
