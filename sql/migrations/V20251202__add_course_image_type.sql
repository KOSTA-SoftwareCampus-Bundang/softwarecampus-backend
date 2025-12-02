-- 과정 이미지 타입 컬럼 추가
-- V20251202__add_course_image_type.sql

-- image_type 컬럼 추가 (THUMBNAIL, HEADER, CONTENT)
ALTER TABLE course_image 
ADD COLUMN image_type VARCHAR(20) DEFAULT 'THUMBNAIL';

-- 기존 데이터 마이그레이션: is_thumbnail이 true인 경우 THUMBNAIL, 아닌 경우 CONTENT
UPDATE course_image 
SET image_type = CASE 
    WHEN is_thumbnail = 1 THEN 'THUMBNAIL'
    ELSE 'CONTENT'
END;

-- 인덱스 추가 (조회 성능 최적화)
CREATE INDEX idx_course_image_type ON course_image(image_type);
CREATE INDEX idx_course_image_course_type ON course_image(course_id, image_type);
