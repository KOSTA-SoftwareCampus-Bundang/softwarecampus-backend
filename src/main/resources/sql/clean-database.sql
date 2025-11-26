-- --------------------------------------------------------------------
-- Clean Database (모든 데이터 삭제)
-- --------------------------------------------------------------------

-- 외래키 체크 비활성화
SET FOREIGN_KEY_CHECKS = 0;

-- 모든 테이블 데이터 삭제 (자식 테이블부터)
TRUNCATE TABLE review_like;
TRUNCATE TABLE review_section;
TRUNCATE TABLE course_review;
TRUNCATE TABLE course_qna;
TRUNCATE TABLE course_favorite;
TRUNCATE TABLE course_curriculum;
TRUNCATE TABLE comment_recommend;
TRUNCATE TABLE comment;
TRUNCATE TABLE board_recommend;
TRUNCATE TABLE board_attach;
TRUNCATE TABLE board;
TRUNCATE TABLE course;
TRUNCATE TABLE course_category;
TRUNCATE TABLE account;
TRUNCATE TABLE academy;

-- 외래키 체크 재활성화
SET FOREIGN_KEY_CHECKS = 1;
