-- V20251203__add_course_capacity.sql
-- 과정(Course) 테이블에 모집 정원(capacity) 컬럼 추가

ALTER TABLE course 
ADD COLUMN capacity INT DEFAULT 30 COMMENT '모집 정원 (기본값: 30명)';

-- 기존 데이터에 대해 기본값 설정 (선택사항 - 이미 DEFAULT가 있지만 명시적으로 업데이트)
-- UPDATE course SET capacity = 30 WHERE capacity IS NULL;
