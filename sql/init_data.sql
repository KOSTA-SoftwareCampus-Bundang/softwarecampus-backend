-- ============================================
-- SoftwareCampus 초기화용 목데이터
-- 작성일: 2025-12-03
-- 설명: 최신 백엔드 스키마 구조에 맞춘 초기 데이터
-- ============================================

-- 외래 키 제약 조건 비활성화 (데이터 삽입 순서 문제 방지)
SET FOREIGN_KEY_CHECKS = 0;

-- Safe update mode 비활성화 (WHERE 없는 DELETE 허용)
SET SQL_SAFE_UPDATES = 0;

-- ============================================
-- 기존 데이터 삭제 (역순으로 삭제 - 자식 테이블부터)
-- ============================================
DELETE FROM comment_recommend WHERE id > 0 OR id <= 0;
DELETE FROM board_recommend WHERE id > 0 OR id <= 0;
DELETE FROM comment WHERE id > 0 OR id <= 0;
DELETE FROM board_attach WHERE id > 0 OR id <= 0;
DELETE FROM board WHERE id > 0 OR id <= 0;
DELETE FROM banner WHERE id > 0 OR id <= 0;
DELETE FROM course_favorite WHERE id > 0 OR id <= 0;
DELETE FROM course_qna WHERE id > 0 OR id <= 0;
DELETE FROM review_like WHERE id > 0 OR id <= 0;
DELETE FROM review_section WHERE id > 0 OR id <= 0;
DELETE FROM course_review_attachment WHERE id > 0 OR id <= 0;
DELETE FROM course_review WHERE id > 0 OR id <= 0;
DELETE FROM course_image WHERE id > 0 OR id <= 0;
DELETE FROM course_curriculum WHERE id > 0 OR id <= 0;
DELETE FROM course WHERE id > 0 OR id <= 0;
DELETE FROM course_category WHERE id > 0 OR id <= 0;
DELETE FROM academy WHERE id > 0 OR id <= 0;
DELETE FROM account WHERE id > 0 OR id <= 0;

-- Safe update mode 재활성화
SET SQL_SAFE_UPDATES = 1;

-- ============================================
-- 1. 기관(Academy) 데이터
-- ============================================
INSERT INTO academy (id, name, address, business_number, email, phone_number, description, logo_url, website, is_approved, approved_at, rejection_reason, is_deleted, deleted_at, created_at, updated_at) VALUES
(1, '소프트캠퍼스', '서울시 강남구 테헤란로 123', '123-45-67890', 'contact@softcampus.co.kr', '02-1234-5678', '실무 중심의 IT 전문 교육기관입니다. 최신 기술 트렌드를 반영한 커리큘럼을 제공합니다.', '/images/academy/softcampus_logo.png', 'https://www.softcampus.co.kr', 'APPROVED', NOW(), NULL, false, NULL, NOW(), NOW()),
(2, '코딩아카데미', '서울시 서초구 서초대로 456', '234-56-78901', 'info@codingacademy.kr', '02-2345-6789', '취업률 90% 이상의 코딩 전문 교육원입니다.', '/images/academy/codingacademy_logo.png', 'https://www.codingacademy.kr', 'APPROVED', NOW(), NULL, false, NULL, NOW(), NOW()),
(3, '테크부스트', '경기도 성남시 분당구 판교로 789', '345-67-89012', 'hello@techboost.io', '031-3456-7890', '스타트업 실무 경험을 쌓을 수 있는 부트캠프입니다.', '/images/academy/techboost_logo.png', 'https://www.techboost.io', 'APPROVED', NOW(), NULL, false, NULL, NOW(), NOW()),
(4, '디지털배움터', '부산시 해운대구 센텀중앙로 111', '456-78-90123', 'support@digitallearn.co.kr', '051-4567-8901', '온라인/오프라인 병행 교육을 제공하는 교육기관입니다.', '/images/academy/digitallearn_logo.png', 'https://www.digitallearn.co.kr', 'APPROVED', NOW(), NULL, false, NULL, NOW(), NOW()),
(5, '승인대기학원', '서울시 종로구 종로 222', '567-89-01234', 'pending@academy.kr', '02-5678-9012', '승인 대기 중인 교육기관입니다.', NULL, NULL, 'PENDING', NULL, NULL, false, NULL, NOW(), NOW());

-- ============================================
-- 2. 계정(Account) 데이터
-- ============================================
-- 비밀번호: 모두 'password123' (BCrypt 인코딩)
-- BCryptPasswordEncoder().encode("password123") 결과값
INSERT INTO account (id, user_name, password, email, phone_number, account_type, affiliation, position, address, account_approved, academy_id, profile_image, terms_agreed, terms_agreed_at, privacy_agreed, privacy_agreed_at, marketing_agreed, marketing_agreed_at, is_deleted, deleted_at, created_at, updated_at) VALUES
-- 관리자 계정
(1, '관리자', '$2a$10$Q//fVwGyMrwLzn.Nrfpi7OJ.ljEP1n7bJG.iayRW2eHeh21jakk9m', 'admin@softcampus.co.kr', '010-1111-1111', 'ADMIN', '소프트캠퍼스', '시스템관리자', '서울시 강남구', 'APPROVED', NULL, NULL, true, NOW(), true, NOW(), false, NULL, false, NULL, NOW(), NOW()),
-- 기관 계정
(2, '소프트캠퍼스담당자', '$2a$10$Q//fVwGyMrwLzn.Nrfpi7OJ.ljEP1n7bJG.iayRW2eHeh21jakk9m', 'manager@softcampus.co.kr', '010-2222-2222', 'ACADEMY', '소프트캠퍼스', '교육담당', '서울시 강남구', 'APPROVED', 1, NULL, true, NOW(), true, NOW(), true, NOW(), false, NULL, NOW(), NOW()),
(3, '코딩아카데미담당자', '$2a$10$Q//fVwGyMrwLzn.Nrfpi7OJ.ljEP1n7bJG.iayRW2eHeh21jakk9m', 'manager@codingacademy.kr', '010-3333-3333', 'ACADEMY', '코딩아카데미', '매니저', '서울시 서초구', 'APPROVED', 2, NULL, true, NOW(), true, NOW(), false, NULL, false, NULL, NOW(), NOW()),
(4, '테크부스트담당자', '$2a$10$Q//fVwGyMrwLzn.Nrfpi7OJ.ljEP1n7bJG.iayRW2eHeh21jakk9m', 'manager@techboost.io', '010-4444-4444', 'ACADEMY', '테크부스트', '운영팀장', '경기도 성남시', 'APPROVED', 3, NULL, true, NOW(), true, NOW(), true, NOW(), false, NULL, NOW(), NOW()),
-- 일반 사용자 계정
(5, '김개발', '$2a$10$Q//fVwGyMrwLzn.Nrfpi7OJ.ljEP1n7bJG.iayRW2eHeh21jakk9m', 'dev.kim@gmail.com', '010-5555-5555', 'USER', '개발회사', '백엔드개발자', '서울시 마포구', 'APPROVED', NULL, NULL, true, NOW(), true, NOW(), true, NOW(), false, NULL, NOW(), NOW()),
(6, '이프론트', '$2a$10$Q//fVwGyMrwLzn.Nrfpi7OJ.ljEP1n7bJG.iayRW2eHeh21jakk9m', 'front.lee@gmail.com', '010-6666-6666', 'USER', 'IT기업', '프론트개발자', '서울시 영등포구', 'APPROVED', NULL, NULL, true, NOW(), true, NOW(), false, NULL, false, NULL, NOW(), NOW()),
(7, '박데이터', '$2a$10$Q//fVwGyMrwLzn.Nrfpi7OJ.ljEP1n7bJG.iayRW2eHeh21jakk9m', 'data.park@gmail.com', '010-7777-7777', 'USER', '데이터회사', '데이터분석가', '서울시 송파구', 'APPROVED', NULL, NULL, true, NOW(), true, NOW(), true, NOW(), false, NULL, NOW(), NOW()),
(8, '최클라우드', '$2a$10$Q//fVwGyMrwLzn.Nrfpi7OJ.ljEP1n7bJG.iayRW2eHeh21jakk9m', 'cloud.choi@gmail.com', '010-8888-8888', 'USER', '클라우드서비스', '클라우드엔지니어', '경기도 수원시', 'APPROVED', NULL, NULL, true, NOW(), true, NOW(), false, NULL, false, NULL, NOW(), NOW()),
(9, '정취준', '$2a$10$Q//fVwGyMrwLzn.Nrfpi7OJ.ljEP1n7bJG.iayRW2eHeh21jakk9m', 'jobseeker.jung@gmail.com', '010-9999-9999', 'USER', NULL, NULL, '서울시 강서구', 'APPROVED', NULL, NULL, true, NOW(), true, NOW(), true, NOW(), false, NULL, NOW(), NOW()),
(10, '한신입', '$2a$10$Q//fVwGyMrwLzn.Nrfpi7OJ.ljEP1n7bJG.iayRW2eHeh21jakk9m', 'newbie.han@gmail.com', '010-1010-1010', 'USER', NULL, NULL, '인천시 남동구', 'APPROVED', NULL, NULL, true, NOW(), true, NOW(), false, NULL, false, NULL, NOW(), NOW());

-- ============================================
-- 3. 과정 카테고리(CourseCategory) 데이터
-- ============================================
INSERT INTO course_category (id, category_name, category_type, is_deleted, deleted_at, created_at, updated_at) VALUES
-- 재직자 (EMPLOYEE) 카테고리
(1, '프론트엔드', 'EMPLOYEE', false, NULL, NOW(), NOW()),
(2, '백엔드', 'EMPLOYEE', false, NULL, NOW(), NOW()),
(3, '풀스택', 'EMPLOYEE', false, NULL, NOW(), NOW()),
(4, '데이터', 'EMPLOYEE', false, NULL, NOW(), NOW()),
(5, 'AI', 'EMPLOYEE', false, NULL, NOW(), NOW()),
(6, '클라우드', 'EMPLOYEE', false, NULL, NOW(), NOW()),
(7, '보안', 'EMPLOYEE', false, NULL, NOW(), NOW()),
(8, '데브옵스/인프라/툴', 'EMPLOYEE', false, NULL, NOW(), NOW()),
(9, 'SW공학', 'EMPLOYEE', false, NULL, NOW(), NOW()),
-- 취업예정자 (JOB_SEEKER) 카테고리
(10, '프론트엔드', 'JOB_SEEKER', false, NULL, NOW(), NOW()),
(11, '백엔드', 'JOB_SEEKER', false, NULL, NOW(), NOW()),
(12, '풀스택', 'JOB_SEEKER', false, NULL, NOW(), NOW()),
(13, '데이터', 'JOB_SEEKER', false, NULL, NOW(), NOW()),
(14, 'AI', 'JOB_SEEKER', false, NULL, NOW(), NOW()),
(15, '클라우드', 'JOB_SEEKER', false, NULL, NOW(), NOW()),
(16, '보안', 'JOB_SEEKER', false, NULL, NOW(), NOW()),
(17, '데브옵스/인프라/툴', 'JOB_SEEKER', false, NULL, NOW(), NOW()),
(18, 'SW공학', 'JOB_SEEKER', false, NULL, NOW(), NOW());

-- ============================================
-- 4. 과정(Course) 데이터
-- ============================================
INSERT INTO course (id, academy_id, requester_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, capacity, is_kdt, is_nailbaeum, is_offline, requirement, view_count, is_approved, approved_at, rejection_reason, is_deleted, deleted_at, created_at, updated_at) VALUES
-- 재직자 과정 (온라인)
(1, 1, 2, 1, 'React 심화 마스터 과정', '2025-12-01', '2025-12-31', '2026-01-15', '2026-03-15', 500000, '월,수,금', '온라인', 30, true, false, false, '기본적인 JavaScript 지식 필요', 150, 'APPROVED', NOW(), NULL, false, NULL, NOW(), NOW()),
(2, 1, 2, 2, 'Spring Boot 실무 프로젝트', '2025-12-01', '2026-01-15', '2026-02-01', '2026-04-30', 600000, '화,목', '온라인', 25, true, true, false, 'Java 기초 필수', 230, 'APPROVED', NOW(), NULL, false, NULL, NOW(), NOW()),
(3, 2, 3, 4, '데이터 분석 with Python', '2025-12-15', '2026-01-31', '2026-02-15', '2026-04-15', 450000, '월,화,수,목,금', '온라인', 40, true, false, false, '파이썬 기초 권장', 180, 'APPROVED', NOW(), NULL, false, NULL, NOW(), NOW()),
(4, 3, 4, 5, 'ChatGPT API 활용 실습', '2025-12-01', '2025-12-20', '2026-01-05', '2026-02-05', 350000, '토,일', '온라인', 20, false, false, false, 'API 기본 이해 필요', 320, 'APPROVED', NOW(), NULL, false, NULL, NOW(), NOW()),

-- 재직자 과정 (오프라인)
(5, 1, 2, 3, 'Node.js + React 풀스택 부트캠프', '2025-12-01', '2026-01-10', '2026-01-20', '2026-04-20', 800000, '월,화,수,목,금', '서울시 강남구', 20, true, true, true, '프로그래밍 경험 6개월 이상', 95, 'APPROVED', NOW(), NULL, false, NULL, NOW(), NOW()),
(6, 2, 3, 6, 'AWS 클라우드 아키텍처 설계', '2025-12-10', '2026-01-20', '2026-02-01', '2026-03-31', 550000, '화,목,토', '서울시 서초구', 15, true, false, true, 'Linux 기초 필요', 78, 'APPROVED', NOW(), NULL, false, NULL, NOW(), NOW()),
(7, 4, NULL, 7, '정보보안 전문가 양성', '2025-12-01', '2025-12-31', '2026-01-15', '2026-04-15', 700000, '월,수,금', '부산시 해운대구', 18, true, false, true, '네트워크 기초 필수', 65, 'APPROVED', NOW(), NULL, false, NULL, NOW(), NOW()),

-- 취업예정자 과정 (온라인)
(8, 1, 2, 10, 'HTML/CSS/JS 프론트엔드 입문', '2025-12-01', '2026-01-31', '2026-02-10', '2026-05-10', 0, '월,화,수,목,금', '온라인', 50, true, true, false, '컴퓨터 기초 활용 가능자', 420, 'APPROVED', NOW(), NULL, false, NULL, NOW(), NOW()),
(9, 2, 3, 11, 'Java 백엔드 개발자 양성', '2025-12-15', '2026-02-15', '2026-03-01', '2026-08-31', 0, '월,화,수,목,금', '온라인', 35, true, true, false, '국민내일배움카드 소지자', 380, 'APPROVED', NOW(), NULL, false, NULL, NOW(), NOW()),
(10, 3, 4, 14, '머신러닝 엔지니어 입문', '2025-12-01', '2025-12-31', '2026-01-10', '2026-06-10', 0, '월,수,금', '온라인', 30, true, true, false, '수학 기초(선형대수) 권장', 290, 'APPROVED', NOW(), NULL, false, NULL, NOW(), NOW()),

-- 취업예정자 과정 (오프라인)
(11, 1, 2, 12, '풀스택 웹개발자 양성과정 6기', '2025-12-01', '2026-01-15', '2026-02-01', '2026-07-31', 0, '월,화,수,목,금', '서울시 강남구', 25, true, true, true, '고졸 이상, 국민내일배움카드 필수', 510, 'APPROVED', NOW(), NULL, false, NULL, NOW(), NOW()),
(12, 2, 3, 13, '빅데이터 분석가 양성', '2025-12-10', '2026-01-20', '2026-02-03', '2026-07-03', 0, '월,화,수,목,금', '서울시 서초구', 22, true, true, true, '통계 기초 지식 우대', 245, 'APPROVED', NOW(), NULL, false, NULL, NOW(), NOW()),
(13, 4, NULL, 15, 'AWS/GCP 클라우드 엔지니어', '2025-12-20', '2026-02-01', '2026-02-15', '2026-06-15', 0, '월,화,수,목,금', '부산시 해운대구', 20, true, true, true, '컴퓨터공학 전공자 우대', 180, 'APPROVED', NOW(), NULL, false, NULL, NOW(), NOW()),

-- 승인 대기/거절 과정
(14, 1, 2, 8, 'Kubernetes 운영 마스터', '2026-01-01', '2026-02-28', '2026-03-15', '2026-06-15', 650000, '화,목,토', '온라인', 15, true, false, false, 'Docker 경험 필수', 0, 'PENDING', NULL, NULL, false, NULL, NOW(), NOW()),
(15, 3, 4, 9, '소프트웨어 아키텍처 설계', '2026-01-15', '2026-02-28', '2026-03-10', '2026-05-10', 480000, '월,수', '경기도 성남시', 12, false, false, true, '개발 경력 3년 이상', 0, 'REJECTED', NULL, '교육 시설 검증 필요', false, NULL, NOW(), NOW());

-- ============================================
-- 5. 과정 커리큘럼(CourseCurriculum) 데이터
-- ============================================
INSERT INTO course_curriculum (id, course_id, chapter_number, chapter_name, chapter_detail, chapter_time, is_deleted, deleted_at, created_at, updated_at) VALUES
-- React 심화 과정 커리큘럼
(1, 1, 1, 'React 기초 복습', 'JSX, 컴포넌트, Props, State 복습', 8, false, NULL, NOW(), NOW()),
(2, 1, 2, 'Hooks 심화', 'useEffect, useCallback, useMemo, Custom Hooks', 12, false, NULL, NOW(), NOW()),
(3, 1, 3, 'Redux Toolkit', 'Redux 기초부터 Redux Toolkit 활용까지', 10, false, NULL, NOW(), NOW()),
(4, 1, 4, 'React Query', '서버 상태 관리와 캐싱 전략', 8, false, NULL, NOW(), NOW()),
(5, 1, 5, '실전 프로젝트', '포트폴리오용 프로젝트 개발', 20, false, NULL, NOW(), NOW()),

-- Spring Boot 과정 커리큘럼
(6, 2, 1, 'Spring Boot 기초', '프로젝트 구성, 의존성 관리, Auto Configuration', 8, false, NULL, NOW(), NOW()),
(7, 2, 2, 'JPA와 데이터베이스', 'Entity 설계, Repository 패턴, 쿼리 메서드', 12, false, NULL, NOW(), NOW()),
(8, 2, 3, 'REST API 설계', 'RESTful API 설계 원칙, 예외 처리, 문서화', 10, false, NULL, NOW(), NOW()),
(9, 2, 4, '보안과 인증', 'Spring Security, JWT 토큰 인증', 10, false, NULL, NOW(), NOW()),
(10, 2, 5, '배포와 운영', 'Docker 컨테이너화, CI/CD 파이프라인', 8, false, NULL, NOW(), NOW()),

-- 풀스택 과정 커리큘럼
(11, 11, 1, 'HTML/CSS 기초', '시맨틱 HTML, CSS Flexbox, Grid 레이아웃', 20, false, NULL, NOW(), NOW()),
(12, 11, 2, 'JavaScript 심화', 'ES6+, 비동기 프로그래밍, DOM 조작', 40, false, NULL, NOW(), NOW()),
(13, 11, 3, 'React 프론트엔드', 'React 기초부터 상태관리까지', 60, false, NULL, NOW(), NOW()),
(14, 11, 4, 'Node.js 백엔드', 'Express.js, RESTful API, 인증/인가', 60, false, NULL, NOW(), NOW()),
(15, 11, 5, '데이터베이스', 'MySQL, MongoDB 기초 및 활용', 40, false, NULL, NOW(), NOW()),
(16, 11, 6, '팀 프로젝트', '실전 프로젝트 기획부터 배포까지', 80, false, NULL, NOW(), NOW());

-- ============================================
-- 6. 과정 이미지(CourseImage) 데이터
-- ============================================
INSERT INTO course_image (id, course_id, image_url, is_thumbnail, image_type, original_filename, is_deleted, deleted_at, created_at, updated_at) VALUES
(1, 1, '/images/courses/react-advanced-thumb.jpg', true, 'THUMBNAIL', 'react-advanced-thumb.jpg', false, NULL, NOW(), NOW()),
(2, 1, '/images/courses/react-advanced-header.jpg', false, 'HEADER', 'react-advanced-header.jpg', false, NULL, NOW(), NOW()),
(3, 2, '/images/courses/spring-boot-thumb.jpg', true, 'THUMBNAIL', 'spring-boot-thumb.jpg', false, NULL, NOW(), NOW()),
(4, 3, '/images/courses/python-data-thumb.jpg', true, 'THUMBNAIL', 'python-data-thumb.jpg', false, NULL, NOW(), NOW()),
(5, 4, '/images/courses/chatgpt-api-thumb.jpg', true, 'THUMBNAIL', 'chatgpt-api-thumb.jpg', false, NULL, NOW(), NOW()),
(6, 5, '/images/courses/fullstack-bootcamp-thumb.jpg', true, 'THUMBNAIL', 'fullstack-bootcamp-thumb.jpg', false, NULL, NOW(), NOW()),
(7, 6, '/images/courses/aws-architecture-thumb.jpg', true, 'THUMBNAIL', 'aws-architecture-thumb.jpg', false, NULL, NOW(), NOW()),
(8, 7, '/images/courses/security-thumb.jpg', true, 'THUMBNAIL', 'security-thumb.jpg', false, NULL, NOW(), NOW()),
(9, 8, '/images/courses/frontend-intro-thumb.jpg', true, 'THUMBNAIL', 'frontend-intro-thumb.jpg', false, NULL, NOW(), NOW()),
(10, 9, '/images/courses/java-backend-thumb.jpg', true, 'THUMBNAIL', 'java-backend-thumb.jpg', false, NULL, NOW(), NOW()),
(11, 10, '/images/courses/ml-intro-thumb.jpg', true, 'THUMBNAIL', 'ml-intro-thumb.jpg', false, NULL, NOW(), NOW()),
(12, 11, '/images/courses/fullstack-dev-thumb.jpg', true, 'THUMBNAIL', 'fullstack-dev-thumb.jpg', false, NULL, NOW(), NOW()),
(13, 12, '/images/courses/bigdata-thumb.jpg', true, 'THUMBNAIL', 'bigdata-thumb.jpg', false, NULL, NOW(), NOW()),
(14, 13, '/images/courses/cloud-engineer-thumb.jpg', true, 'THUMBNAIL', 'cloud-engineer-thumb.jpg', false, NULL, NOW(), NOW());

-- ============================================
-- 7. 과정 리뷰(CourseReview) 데이터
-- ============================================
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, rejection_reason, type, is_deleted, deleted_at, created_at, updated_at) VALUES
(1, 5, 1, 'React 심화 과정 덕분에 실무에서 바로 적용할 수 있었습니다. 특히 Redux Toolkit 부분이 매우 유용했어요.', 'APPROVED', NULL, 'EMPLOYEE', false, NULL, NOW(), NOW()),
(2, 6, 1, '커스텀 훅 작성법을 배우고 코드 재사용성이 많이 높아졌습니다. 강추!', 'APPROVED', NULL, 'EMPLOYEE', false, NULL, NOW(), NOW()),
(3, 5, 2, 'Spring Boot와 JPA를 체계적으로 배울 수 있어서 좋았습니다.', 'APPROVED', NULL, 'EMPLOYEE', false, NULL, NOW(), NOW()),
(4, 7, 3, '파이썬 데이터 분석 과정이 실무에 바로 적용할 수 있는 내용이라 만족합니다.', 'APPROVED', NULL, 'EMPLOYEE', false, NULL, NOW(), NOW()),
(5, 8, 6, 'AWS 아키텍처 설계 능력이 많이 향상되었습니다. 비용 최적화 부분도 유익했어요.', 'APPROVED', NULL, 'EMPLOYEE', false, NULL, NOW(), NOW()),
(6, 9, 11, '비전공자도 따라갈 수 있는 커리큘럼이었습니다. 취업까지 연결되어 감사합니다!', 'APPROVED', NULL, 'JOB_SEEKER', false, NULL, NOW(), NOW()),
(7, 10, 8, 'HTML/CSS부터 차근차근 배워서 좋았어요. 포트폴리오도 만들 수 있었습니다.', 'APPROVED', NULL, 'JOB_SEEKER', false, NULL, NOW(), NOW()),
(8, 9, 9, '자바 백엔드 과정 덕분에 원하는 회사에 취업할 수 있었습니다. 감사합니다!', 'APPROVED', NULL, 'JOB_SEEKER', false, NULL, NOW(), NOW()),
-- 승인 대기 리뷰
(9, 10, 10, '머신러닝 과정 수강 후기입니다. 기초부터 탄탄하게 배웠습니다.', 'PENDING', NULL, 'JOB_SEEKER', false, NULL, NOW(), NOW());

-- ============================================
-- 8. 리뷰 섹션(ReviewSection) 데이터
-- ============================================
INSERT INTO review_section (id, review_id, section_type, score, comment, is_deleted, deleted_at, created_at, updated_at) VALUES
-- 리뷰 1의 섹션들
(1, 1, 'CURRICULUM', 5, '실무에 바로 적용할 수 있는 내용', false, NULL, NOW(), NOW()),
(2, 1, 'COURSEWARE', 4, '자료가 체계적으로 정리되어 있음', false, NULL, NOW(), NOW()),
(3, 1, 'INSTRUCTOR', 5, '질문에 성실하게 답변해주심', false, NULL, NOW(), NOW()),
(4, 1, 'EQUIPMENT', 4, '온라인이라 해당없음', false, NULL, NOW(), NOW()),
-- 리뷰 2의 섹션들
(5, 2, 'CURRICULUM', 5, '커스텀 훅 내용이 특히 좋았음', false, NULL, NOW(), NOW()),
(6, 2, 'COURSEWARE', 5, '예제 코드가 풍부함', false, NULL, NOW(), NOW()),
(7, 2, 'INSTRUCTOR', 5, '설명이 명확함', false, NULL, NOW(), NOW()),
(8, 2, 'EQUIPMENT', 4, NULL, false, NULL, NOW(), NOW()),
-- 리뷰 3의 섹션들
(9, 3, 'CURRICULUM', 4, 'JPA 심화 내용이 더 있었으면', false, NULL, NOW(), NOW()),
(10, 3, 'COURSEWARE', 5, '문서화가 잘 되어있음', false, NULL, NOW(), NOW()),
(11, 3, 'INSTRUCTOR', 5, '실무 경험 공유가 좋았음', false, NULL, NOW(), NOW()),
(12, 3, 'EQUIPMENT', 4, NULL, false, NULL, NOW(), NOW()),
-- 리뷰 6의 섹션들 (취업예정자)
(13, 6, 'CURRICULUM', 5, '비전공자 친화적', false, NULL, NOW(), NOW()),
(14, 6, 'COURSEWARE', 5, '단계별로 잘 구성됨', false, NULL, NOW(), NOW()),
(15, 6, 'INSTRUCTOR', 5, '포트폴리오 피드백이 좋았음', false, NULL, NOW(), NOW()),
(16, 6, 'EQUIPMENT', 5, '실습 환경이 좋았음', false, NULL, NOW(), NOW());

-- ============================================
-- 9. 리뷰 좋아요(ReviewLike) 데이터
-- ============================================
INSERT INTO review_like (id, review_id, account_id, type, is_deleted, created_at, updated_at) VALUES
(1, 1, 6, 'LIKE', false, NOW(), NOW()),
(2, 1, 7, 'LIKE', false, NOW(), NOW()),
(3, 2, 5, 'LIKE', false, NOW(), NOW()),
(4, 3, 6, 'LIKE', false, NOW(), NOW()),
(5, 6, 10, 'LIKE', false, NOW(), NOW()),
(6, 6, 7, 'LIKE', false, NOW(), NOW()),
(7, 7, 9, 'LIKE', false, NOW(), NOW());

-- ============================================
-- 10. 과정 Q&A(CourseQna) 데이터
-- ============================================
INSERT INTO course_qna (id, course_id, account_id, title, question_text, answer_text, answered_by_id, is_answered, is_deleted, deleted_at, created_at, updated_at) VALUES
(1, 1, 9, 'React 버전 문의', 'React 몇 버전을 사용하나요?', 'React 18 버전을 사용합니다.', 2, true, false, NULL, NOW(), NOW()),
(2, 1, 10, '사전 지식 문의', 'JavaScript 기초만 알아도 수강 가능한가요?', 'ES6+ 문법까지 알고 계시면 수강 가능합니다.', 2, true, false, NULL, NOW(), NOW()),
(3, 2, 5, 'JPA vs MyBatis', '이 과정에서 MyBatis도 다루나요?', 'JPA 위주로 진행하며, MyBatis는 간략히 소개합니다.', 2, true, false, NULL, NOW(), NOW()),
(4, 11, 9, '취업 연계 문의', '취업 연계 프로그램이 있나요?', '네, 수료 후 협력 기업 면접 기회가 제공됩니다.', 2, true, false, NULL, NOW(), NOW()),
(5, 8, 10, '수료증 발급', '수료증 발급이 되나요?', NULL, NULL, false, false, NULL, NOW(), NOW());

-- ============================================
-- 11. 과정 즐겨찾기(CourseFavorite) 데이터
-- ============================================
INSERT INTO course_favorite (id, account_id, course_id, created_at, updated_at) VALUES
(1, 5, 1, NOW(), NOW()),
(2, 5, 2, NOW(), NOW()),
(3, 6, 1, NOW(), NOW()),
(4, 6, 8, NOW(), NOW()),
(5, 7, 3, NOW(), NOW()),
(6, 7, 4, NOW(), NOW()),
(7, 8, 6, NOW(), NOW()),
(8, 9, 11, NOW(), NOW()),
(9, 9, 9, NOW(), NOW()),
(10, 10, 8, NOW(), NOW()),
(11, 10, 11, NOW(), NOW());

-- ============================================
-- 12. 배너(Banner) 데이터
-- ============================================
INSERT INTO banner (id, title, description, image_url, link_url, sequence, is_activated, is_deleted, deleted_at, created_at, updated_at) VALUES
(1, '2026년 상반기 국비지원 모집', 'K-Digital Training 과정 신규 오픈! 지금 바로 신청하세요', '/images/banners/kdt-2026.jpg', '/courses?categoryType=JOB_SEEKER', 1, true, false, NULL, NOW(), NOW()),
(2, '재직자 AI 역량 강화', '직장인을 위한 ChatGPT & AI 활용 과정 특별 할인', '/images/banners/ai-worker.jpg', '/courses?categoryType=EMPLOYEE&category=AI', 2, true, false, NULL, NOW(), NOW()),
(3, '풀스택 부트캠프 6기 모집', '6개월 완성 웹개발자 양성과정 - 선착순 마감', '/images/banners/fullstack-6.jpg', '/course/11', 3, true, false, NULL, NOW(), NOW()),
(4, '클라우드 엔지니어 과정 오픈', 'AWS/GCP 자격증 취득까지 한번에', '/images/banners/cloud-cert.jpg', '/courses?categoryType=EMPLOYEE&category=클라우드', 4, true, false, NULL, NOW(), NOW()),
(5, '비활성 배너 예시', '이 배너는 노출되지 않습니다', '/images/banners/inactive.jpg', '/', 5, false, false, NULL, NOW(), NOW());

-- ============================================
-- 13. 게시판(Board) 데이터
-- ============================================
INSERT INTO board (id, account_id, category, title, text, hits, is_secret, is_deleted, deleted_at, created_at, updated_at) VALUES
-- 공지사항
(1, 1, 'NOTICE', '[공지] 소프트캠퍼스 이용안내', '<p>안녕하세요, 소프트캠퍼스입니다.</p><p>원활한 서비스 이용을 위한 안내사항입니다.</p>', 520, false, false, NULL, NOW(), NOW()),
(2, 1, 'NOTICE', '[공지] 2025년 연말 휴무 안내', '<p>12월 25일 ~ 1월 1일까지 고객센터 휴무입니다.</p>', 340, false, false, NULL, NOW(), NOW()),
(3, 1, 'NOTICE', '[공지] 개인정보처리방침 변경 안내', '<p>2025년 12월 1일부로 개인정보처리방침이 변경되었습니다.</p>', 180, false, false, NULL, NOW(), NOW()),

-- 문의사항
(4, 5, 'QUESTION', '수강 신청 관련 문의드립니다', '<p>React 심화 과정 수강 신청은 어떻게 하나요?</p>', 45, false, false, NULL, NOW(), NOW()),
(5, 6, 'QUESTION', '환불 규정 문의', '<p>개강 전 환불 가능한가요?</p>', 32, false, false, NULL, NOW(), NOW()),
(6, 9, 'QUESTION', '비밀 문의입니다', '<p>개인적인 문의사항입니다.</p>', 5, true, false, NULL, NOW(), NOW()),

-- 진로이야기
(7, 5, 'COURSE_STORY', '백엔드 개발자 취업 후기', '<p>6개월 과정 수료 후 백엔드 개발자로 취업했습니다!</p><p>과정에서 배운 Spring Boot 덕분에 실무 적응이 빨랐어요.</p>', 280, false, false, NULL, NOW(), NOW()),
(8, 9, 'COURSE_STORY', '비전공자 개발자 전향기', '<p>문과 출신으로 개발자가 된 이야기를 공유합니다.</p>', 450, false, false, NULL, NOW(), NOW()),
(9, 6, 'COURSE_STORY', '프론트엔드 개발 1년차 회고', '<p>프론트엔드 개발자로 1년간 일하며 느낀점을 정리했습니다.</p>', 320, false, false, NULL, NOW(), NOW()),

-- 코딩이야기
(10, 5, 'CODING_STORY', 'JavaScript 클로저 이해하기', '<p>클로저 개념을 쉽게 설명해봅니다.</p><pre>function outer() { ... }</pre>', 180, false, false, NULL, NOW(), NOW()),
(11, 7, 'CODING_STORY', 'Python 리스트 컴프리헨션 팁', '<p>파이썬 리스트 컴프리헨션 활용 팁 공유합니다.</p>', 120, false, false, NULL, NOW(), NOW()),
(12, 8, 'CODING_STORY', 'Docker 입문자를 위한 가이드', '<p>Docker 처음 시작하시는 분들을 위한 글입니다.</p>', 250, false, false, NULL, NOW(), NOW());

-- ============================================
-- 14. 댓글(Comment) 데이터
-- ============================================
INSERT INTO comment (id, board_id, account_id, comment_id, text, is_secret, is_deleted, deleted_at, created_at, updated_at) VALUES
-- 공지사항 댓글
(1, 1, 5, NULL, '유용한 정보 감사합니다!', false, false, NULL, NOW(), NOW()),
(2, 1, 6, NULL, '잘 읽었습니다.', false, false, NULL, NOW(), NOW()),

-- 문의사항 댓글 (답변)
(3, 4, 1, NULL, '수강 신청은 과정 상세 페이지에서 가능합니다. 감사합니다.', false, false, NULL, NOW(), NOW()),
(4, 5, 1, NULL, '개강 7일 전까지 전액 환불 가능합니다.', false, false, NULL, NOW(), NOW()),

-- 진로이야기 댓글
(5, 7, 9, NULL, '축하드립니다! 저도 힘내볼게요.', false, false, NULL, NOW(), NOW()),
(6, 7, 10, NULL, '과정 추천 부탁드려요!', false, false, NULL, NOW(), NOW()),
(7, 7, 5, 6, '풀스택 웹개발자 양성과정 추천드립니다.', false, false, NULL, NOW(), NOW()),
(8, 8, 5, NULL, '용기 주는 글 감사합니다.', false, false, NULL, NOW(), NOW()),
(9, 8, 6, NULL, '저도 비전공자인데 많은 도움이 됐어요.', false, false, NULL, NOW(), NOW()),

-- 코딩이야기 댓글
(10, 10, 6, NULL, '클로저 설명이 정말 명확하네요!', false, false, NULL, NOW(), NOW()),
(11, 10, 7, NULL, '예제 코드 감사합니다.', false, false, NULL, NOW(), NOW()),
(12, 12, 5, NULL, 'Docker 입문에 큰 도움이 됐습니다.', false, false, NULL, NOW(), NOW());

-- ============================================
-- 15. 게시글 추천(BoardRecommend) 데이터
-- ============================================
INSERT INTO board_recommend (id, board_id, account_id, created_at) VALUES
(1, 7, 6, NOW()),
(2, 7, 9, NOW()),
(3, 7, 10, NOW()),
(4, 8, 5, NOW()),
(5, 8, 6, NOW()),
(6, 8, 7, NOW()),
(7, 8, 10, NOW()),
(8, 10, 6, NOW()),
(9, 10, 7, NOW()),
(10, 12, 5, NOW()),
(11, 12, 9, NOW());

-- ============================================
-- 16. 댓글 추천(CommentRecommend) 데이터
-- ============================================
INSERT INTO comment_recommend (id, comment_id, account_id, created_at, updated_at) VALUES
(1, 3, 5, NOW(), NOW()),
(2, 4, 6, NOW(), NOW()),
(3, 5, 7, NOW(), NOW()),
(4, 8, 9, NOW(), NOW());

-- 외래 키 제약 조건 재활성화
SET FOREIGN_KEY_CHECKS = 1;

-- AUTO_INCREMENT 값 설정 (다음 INSERT가 정상 동작하도록)
ALTER TABLE academy AUTO_INCREMENT = 100;
ALTER TABLE account AUTO_INCREMENT = 100;
ALTER TABLE course_category AUTO_INCREMENT = 100;
ALTER TABLE course AUTO_INCREMENT = 100;
ALTER TABLE course_curriculum AUTO_INCREMENT = 100;
ALTER TABLE course_image AUTO_INCREMENT = 100;
ALTER TABLE course_review AUTO_INCREMENT = 100;
ALTER TABLE review_section AUTO_INCREMENT = 100;
ALTER TABLE review_like AUTO_INCREMENT = 100;
ALTER TABLE course_qna AUTO_INCREMENT = 100;
ALTER TABLE course_favorite AUTO_INCREMENT = 100;
ALTER TABLE banner AUTO_INCREMENT = 100;
ALTER TABLE board AUTO_INCREMENT = 100;
ALTER TABLE comment AUTO_INCREMENT = 100;
ALTER TABLE board_recommend AUTO_INCREMENT = 100;
ALTER TABLE comment_recommend AUTO_INCREMENT = 100;

-- ============================================
-- 데이터 확인용 쿼리 (선택적 실행)
-- ============================================
-- SELECT '=== 데이터 삽입 완료 ===' AS message;
-- SELECT 'Academy' AS entity, COUNT(*) AS count FROM academy WHERE is_deleted = false
-- UNION ALL SELECT 'Account', COUNT(*) FROM account WHERE is_deleted = false
-- UNION ALL SELECT 'CourseCategory', COUNT(*) FROM course_category WHERE is_deleted = false
-- UNION ALL SELECT 'Course', COUNT(*) FROM course WHERE is_deleted = false
-- UNION ALL SELECT 'CourseReview', COUNT(*) FROM course_review WHERE is_deleted = false
-- UNION ALL SELECT 'Banner', COUNT(*) FROM banner WHERE is_deleted = false
-- UNION ALL SELECT 'Board', COUNT(*) FROM board WHERE is_deleted = false;
