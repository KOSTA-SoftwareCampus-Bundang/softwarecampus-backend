-- 마이그레이션: Academy 및 Banner 테이블 필드 추가
-- 작성일: 2025-12-02
-- 목적: 프론트엔드에서 필요한 추가 정보 필드 지원

-- Academy 테이블에 추가 필드
ALTER TABLE academy 
ADD COLUMN IF NOT EXISTS description TEXT COMMENT '기관 소개',
ADD COLUMN IF NOT EXISTS logo_url VARCHAR(500) COMMENT '기관 로고 URL',
ADD COLUMN IF NOT EXISTS website VARCHAR(500) COMMENT '기관 웹사이트 URL';

-- Banner 테이블에 description 필드가 없는 경우 추가
-- (이미 있을 수 있으므로 IF NOT EXISTS 사용)
ALTER TABLE banner
ADD COLUMN IF NOT EXISTS description VARCHAR(500) COMMENT '배너 부제목/설명';
