use softcampus_db;

-- --------------------------------------------------------------------
-- Mock Data for Software Campus (2025-12-02 기준 최신 스키마)
-- --------------------------------------------------------------------

-- 1. Academy (훈련기관 정보)
INSERT INTO academy (id, name, address, business_number, email, phone_number, description, logo_url, website, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (1, 'KOSTA 종로', '서울특별시 종로구 우정국로 2길 21 대왕빌딩 7층', '031-606-9321', 'jongno@kosta.com', '02-6070-9301', '전통적인 IT 교육 경험과 기업 맞춤형 커리큘럼을 운영하는 종합 훈련기관입니다.', 'https://assets.softwarecampus.kr/logo-jongno.svg', 'https://jongno.softwarecampus.kr', 'APPROVED', NOW(), NOW(), NOW(), false);

INSERT INTO academy (id, name, address, business_number, email, phone_number, description, logo_url, website, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (2, 'KOSTA 가산', '서울특별시 금천구 가산디지털1로 70 호서대벤처타워 9층', '02-6278-9357', 'gasan@kosta.com', '02-6070-9302', '기업 연계 프로젝트를 중심으로 실무형 인재를 양성하는 가산 캠퍼스입니다.', 'https://assets.softwarecampus.kr/logo-gasan.svg', 'https://gasan.softwarecampus.kr', 'APPROVED', NOW(), NOW(), NOW(), false);

INSERT INTO academy (id, name, address, business_number, email, phone_number, description, logo_url, website, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (3, 'KOSTA 분당', '경기도 성남시 성남대로 34 하나플라자 6층', '031-606-9311', 'bundang@kosta.com', '031-6070-9303', '국내 최대 규모의 융합 교육 인프라와 스타트업 연계 스탠딩 랩을 운영하는 분당 캠퍼스입니다.', 'https://assets.softwarecampus.kr/logo-bundang.svg', 'https://bundang.softwarecampus.kr', 'APPROVED', NOW(), NOW(), NOW(), false);

-- 2. CourseCategory (카테고리 정보)
INSERT INTO course_category (id, category_name, category_type, created_at, updated_at, is_deleted)
VALUES (1, '웹개발', 'JOB_SEEKER', NOW(), NOW(), false);
INSERT INTO course_category (id, category_name, category_type, created_at, updated_at, is_deleted)
VALUES (2, '모바일', 'JOB_SEEKER', NOW(), NOW(), false);
INSERT INTO course_category (id, category_name, category_type, created_at, updated_at, is_deleted)
VALUES (3, '데이터/AI', 'JOB_SEEKER', NOW(), NOW(), false);
INSERT INTO course_category (id, category_name, category_type, created_at, updated_at, is_deleted)
VALUES (4, '클라우드', 'JOB_SEEKER', NOW(), NOW(), false);
INSERT INTO course_category (id, category_name, category_type, created_at, updated_at, is_deleted)
VALUES (5, '보안', 'JOB_SEEKER', NOW(), NOW(), false);
INSERT INTO course_category (id, category_name, category_type, created_at, updated_at, is_deleted)
VALUES (6, '풀스택', 'JOB_SEEKER', NOW(), NOW(), false);
INSERT INTO course_category (id, category_name, category_type, created_at, updated_at, is_deleted)
VALUES (7, '백엔드개발자', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_category (id, category_name, category_type, created_at, updated_at, is_deleted)
VALUES (8, '프론트엔드개발자', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_category (id, category_name, category_type, created_at, updated_at, is_deleted)
VALUES (9, '데이터분석가', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_category (id, category_name, category_type, created_at, updated_at, is_deleted)
VALUES (10, 'AI엔지니어', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_category (id, category_name, category_type, created_at, updated_at, is_deleted)
VALUES (11, '클라우드엔지니어', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_category (id, category_name, category_type, created_at, updated_at, is_deleted)
VALUES (12, '보안엔지니어', 'EMPLOYEE', NOW(), NOW(), false);

-- 3. Account (관리자/회원/기관 담당자)
INSERT INTO account (id, user_name, password, email, phone_number, account_type, affiliation, position, address, account_approved, academy_id, terms_agreed, terms_agreed_at, privacy_agreed, privacy_agreed_at, marketing_agreed, marketing_agreed_at, profile_image, created_at, updated_at, is_deleted, deleted_at)
VALUES (2, '소프트캠퍼스 관리자', '$2a$10$KxYpO6W1l1L9RZyyuVwlyeO6bC3ukYqT/jmU5li6gTQzY/7KgTYkK', 'admin@softwarecampus.kr', '010-0000-0002', 'ADMIN', NULL, NULL, '서울특별시 종로구', 'APPROVED', NULL, true, NOW(), true, NOW(), true, NOW(), 'https://assets.softwarecampus.kr/profile-admin.png', NOW(), NOW(), false, NULL);

INSERT INTO account (id, user_name, password, email, phone_number, account_type, affiliation, position, address, account_approved, academy_id, terms_agreed, terms_agreed_at, privacy_agreed, privacy_agreed_at, marketing_agreed, marketing_agreed_at, profile_image, created_at, updated_at, is_deleted, deleted_at)
VALUES (3, '김철수', '$2a$10$KxYpO6W1l1L9RZyyuVwlyeO6bC3ukYqT/jmU5li6gTQzY/7KgTYkK', 'chulsoo@test.com', '010-2222-3333', 'USER', NULL, NULL, '서울특별시 강남구', 'APPROVED', NULL, true, NOW(), true, NOW(), false, NULL, 'https://assets.softwarecampus.kr/profile-03.png', NOW(), NOW(), false, NULL);

INSERT INTO account (id, user_name, password, email, phone_number, account_type, affiliation, position, address, account_approved, academy_id, terms_agreed, terms_agreed_at, privacy_agreed, privacy_agreed_at, marketing_agreed, marketing_agreed_at, profile_image, created_at, updated_at, is_deleted, deleted_at)
VALUES (4, '이영희', '$2a$10$KxYpO6W1l1L9RZyyuVwlyeO6bC3ukYqT/jmU5li6gTQzY/7KgTYkK', 'younghee@test.com', '010-4444-5555', 'USER', NULL, NULL, '서울특별시 송파구', 'APPROVED', NULL, true, NOW(), true, NOW(), true, NOW(), 'https://assets.softwarecampus.kr/profile-04.png', NOW(), NOW(), false, NULL);

INSERT INTO account (id, user_name, password, email, phone_number, account_type, affiliation, position, address, account_approved, academy_id, terms_agreed, terms_agreed_at, privacy_agreed, privacy_agreed_at, marketing_agreed, marketing_agreed_at, profile_image, created_at, updated_at, is_deleted, deleted_at)
VALUES (5, '박민수', '$2a$10$KxYpO6W1l1L9RZyyuVwlyeO6bC3ukYqT/jmU5li6gTQzY/7KgTYkK', 'minsoo@test.com', '010-6666-7777', 'USER', NULL, NULL, '서울특별시 마포구', 'APPROVED', NULL, true, NOW(), true, NOW(), false, NULL, 'https://assets.softwarecampus.kr/profile-05.png', NOW(), NOW(), false, NULL);

INSERT INTO account (id, user_name, password, email, phone_number, account_type, affiliation, position, address, account_approved, academy_id, terms_agreed, terms_agreed_at, privacy_agreed, privacy_agreed_at, marketing_agreed, marketing_agreed_at, profile_image, created_at, updated_at, is_deleted, deleted_at)
VALUES (6, '최지은', '$2a$10$KxYpO6W1l1L9RZyyuVwlyeO6bC3ukYqT/jmU5li6gTQzY/7KgTYkK', 'jieun@test.com', '010-8888-9999', 'USER', NULL, NULL, '서울특별시 서대문구', 'APPROVED', NULL, true, NOW(), true, NOW(), false, NULL, 'https://assets.softwarecampus.kr/profile-06.png', NOW(), NOW(), false, NULL);

INSERT INTO account (id, user_name, password, email, phone_number, account_type, affiliation, position, address, account_approved, academy_id, terms_agreed, terms_agreed_at, privacy_agreed, privacy_agreed_at, marketing_agreed, marketing_agreed_at, profile_image, created_at, updated_at, is_deleted, deleted_at)
VALUES (7, '허민재', '$2a$10$KxYpO6W1l1L9RZyyuVwlyeO6bC3ukYqT/jmU5li6gTQzY/7KgTYkK', 'minjae@test.com', '010-1212-3434', 'USER', NULL, NULL, '경기도 성남시 분당구', 'APPROVED', NULL, true, NOW(), true, NOW(), false, NULL, 'https://assets.softwarecampus.kr/profile-07.png', NOW(), NOW(), false, NULL);

INSERT INTO account (id, user_name, password, email, phone_number, account_type, affiliation, position, address, account_approved, academy_id, terms_agreed, terms_agreed_at, privacy_agreed, privacy_agreed_at, marketing_agreed, marketing_agreed_at, profile_image, created_at, updated_at, is_deleted, deleted_at)
VALUES (8, '안선영', '$2a$10$KxYpO6W1l1L9RZyyuVwlyeO6bC3ukYqT/jmU5li6gTQzY/7KgTYkK', 'seyoung@test.com', '010-5656-7878', 'USER', NULL, NULL, '서울특별시 강서구', 'APPROVED', NULL, true, NOW(), true, NOW(), false, NULL, 'https://assets.softwarecampus.kr/profile-08.png', NOW(), NOW(), false, NULL);

INSERT INTO account (id, user_name, password, email, phone_number, account_type, affiliation, position, address, account_approved, academy_id, terms_agreed, terms_agreed_at, privacy_agreed, privacy_agreed_at, marketing_agreed, marketing_agreed_at, profile_image, created_at, updated_at, is_deleted, deleted_at)
VALUES (9, '나현우', '$2a$10$KxYpO6W1l1L9RZyyuVwlyeO6bC3ukYqT/jmU5li6gTQzY/7KgTYkK', 'hyunwoo@test.com', '010-3434-5656', 'USER', NULL, NULL, '서울특별시 영등포구', 'APPROVED', NULL, true, NOW(), true, NOW(), false, NULL, 'https://assets.softwarecampus.kr/profile-09.png', NOW(), NOW(), false, NULL);

INSERT INTO account (id, user_name, password, email, phone_number, account_type, affiliation, position, address, account_approved, academy_id, terms_agreed, terms_agreed_at, privacy_agreed, privacy_agreed_at, marketing_agreed, marketing_agreed_at, profile_image, created_at, updated_at, is_deleted, deleted_at)
VALUES (10, '이준호', '$2a$10$KxYpO6W1l1L9RZyyuVwlyeO6bC3ukYqT/jmU5li6gTQzY/7KgTYkK', 'junho@test.com', '010-7878-9090', 'USER', NULL, NULL, '경기도 고양시 일산동구', 'APPROVED', NULL, true, NOW(), true, NOW(), false, NULL, 'https://assets.softwarecampus.kr/profile-10.png', NOW(), NOW(), false, NULL);

INSERT INTO account (id, user_name, password, email, phone_number, account_type, affiliation, position, address, account_approved, academy_id, terms_agreed, terms_agreed_at, privacy_agreed, privacy_agreed_at, marketing_agreed, marketing_agreed_at, profile_image, created_at, updated_at, is_deleted, deleted_at)
VALUES (11, '정다은', '$2a$10$KxYpO6W1l1L9RZyyuVwlyeO6bC3ukYqT/jmU5li6gTQzY/7KgTYkK', 'daeun@test.com', '010-2121-4343', 'USER', NULL, NULL, '서울특별시 동작구', 'APPROVED', NULL, true, NOW(), true, NOW(), false, NULL, 'https://assets.softwarecampus.kr/profile-11.png', NOW(), NOW(), false, NULL);

INSERT INTO account (id, user_name, password, email, phone_number, account_type, affiliation, position, address, account_approved, academy_id, terms_agreed, terms_agreed_at, privacy_agreed, privacy_agreed_at, marketing_agreed, marketing_agreed_at, profile_image, created_at, updated_at, is_deleted, deleted_at)
VALUES (12, '한지원', '$2a$10$KxYpO6W1l1L9RZyyuVwlyeO6bC3ukYqT/jmU5li6gTQzY/7KgTYkK', 'jiyoon@test.com', '010-9090-1234', 'USER', NULL, NULL, '경기도 성남시 수정구', 'APPROVED', NULL, true, NOW(), true, NOW(), true, NOW(), 'https://assets.softwarecampus.kr/profile-12.png', NOW(), NOW(), false, NULL);

INSERT INTO account (id, user_name, password, email, phone_number, account_type, affiliation, position, address, account_approved, academy_id, terms_agreed, terms_agreed_at, privacy_agreed, privacy_agreed_at, marketing_agreed, marketing_agreed_at, profile_image, created_at, updated_at, is_deleted, deleted_at)
VALUES (13, '김정훈 (종로 담당)', '$2a$10$KxYpO6W1l1L9RZyyuVwlyeO6bC3ukYqT/jmU5li6gTQzY/7KgTYkK', 'jongno-manager@softwarecampus.kr', '010-1212-1212', 'ACADEMY', 'KOSTA 종로', '교육운영팀장', '서울특별시 종로구', 'APPROVED', 1, true, NOW(), true, NOW(), true, NOW(), NULL, NOW(), NOW(), false, NULL);

INSERT INTO account (id, user_name, password, email, phone_number, account_type, affiliation, position, address, account_approved, academy_id, terms_agreed, terms_agreed_at, privacy_agreed, privacy_agreed_at, marketing_agreed, marketing_agreed_at, profile_image, created_at, updated_at, is_deleted, deleted_at)
VALUES (14, '김은지 (가산 담당)', '$2a$10$KxYpO6W1l1L9RZyyuVwlyeO6bC3ukYqT/jmU5li6gTQzY/7KgTYkK', 'gasan-manager@softwarecampus.kr', '010-3434-3434', 'ACADEMY', 'KOSTA 가산', '교육담당', '서울특별시 금천구', 'APPROVED', 2, true, NOW(), true, NOW(), true, NOW(), NULL, NOW(), NOW(), false, NULL);

INSERT INTO account (id, user_name, password, email, phone_number, account_type, affiliation, position, address, account_approved, academy_id, terms_agreed, terms_agreed_at, privacy_agreed, privacy_agreed_at, marketing_agreed, marketing_agreed_at, profile_image, created_at, updated_at, is_deleted, deleted_at)
VALUES (15, '윤상혁 (분당 담당)', '$2a$10$KxYpO6W1l1L9RZyyuVwlyeO6bC3ukYqT/jmU5li6gTQzY/7KgTYkK', 'bundang-manager@softwarecampus.kr', '010-5656-5656', 'ACADEMY', 'KOSTA 분당', '교육지원', '경기도 성남시 분당구', 'APPROVED', 3, true, NOW(), true, NOW(), true, NOW(), NULL, NOW(), NOW(), false, NULL);

-- 4. Course (다양한 모집/강의 조건 포함)
INSERT INTO course (id, academy_id, requester_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, view_count, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (1, 1, 13, 7, '엔터프라이즈 Java 백엔드 아키텍처', '2025-11-10', '2025-12-05', '2026-01-06', '2026-05-31', 3600000, '화목 19:00~22:30', '온라인', false, true, false, 'Java 기초 및 객체 지향 지식', 1240, 'APPROVED', NOW(), NOW(), NOW(), false);

INSERT INTO course (id, academy_id, requester_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, view_count, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (2, 2, 14, 8, '프론트엔드 아키텍처와 실전', '2025-10-24', '2025-11-30', '2025-12-03', '2026-03-29', 3200000, '월수금 20:00~22:30', '온라인', false, false, false, 'HTML/CSS/JavaScript 기본', 875, 'APPROVED', NOW(), NOW(), NOW(), false);

INSERT INTO course (id, academy_id, requester_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, view_count, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (3, 3, 15, 9, 'AI 데이터 분석 실무', '2025-12-01', '2025-12-31', '2026-01-08', '2026-06-12', 4100000, '화목 19:30~22:00', '온라인', false, true, false, 'Python 기초 및 통계 이해', 920, 'APPROVED', NOW(), NOW(), NOW(), false);

INSERT INTO course (id, academy_id, requester_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, view_count, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (4, 1, 13, 11, '클라우드 네이티브 인프라 설계', '2025-10-15', '2025-11-10', '2025-11-17', '2026-02-28', 3800000, '주말 09:30~17:30', '서울특별시 종로구 우정국로 2길 21', true, false, true, 'Linux/네트워크 이해', 640, 'APPROVED', NOW(), NOW(), NOW(), false);

INSERT INTO course (id, academy_id, requester_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, view_count, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (5, 2, 14, 2, '모바일 앱 기획부터 배포', '2025-11-01', '2025-11-25', '2025-12-02', '2026-03-29', 2700000, '화목 19:00~22:00', '온라인', false, false, false, 'Sketch/Proto 경험', 510, 'APPROVED', NOW(), NOW(), NOW(), false);

INSERT INTO course (id, academy_id, requester_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, view_count, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (6, 3, 15, 10, 'AI 엔지니어링 연구 전환 트랙', '2025-12-10', '2025-12-31', '2026-01-12', '2026-05-02', 4500000, '월수금 18:30~22:30', '온라인', false, true, false, '선형대수/통계 기초', 470, 'APPROVED', NOW(), NOW(), NOW(), false);

INSERT INTO course (id, academy_id, requester_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, view_count, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (7, 1, 13, 12, '보안 위협 대응 트레이닝', '2025-11-20', '2025-12-18', '2026-01-04', '2026-03-26', 2900000, '주말 10:00~16:00', '서울특별시 종로구 우정국로 2길 21', false, false, true, '네트워크 기본 및 Python', 620, 'APPROVED', NOW(), NOW(), NOW(), false);

INSERT INTO course (id, academy_id, requester_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, view_count, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (8, 2, 14, 6, '풀스택 JavaScript 부트캠프', '2025-10-05', '2025-11-05', '2025-11-10', '2026-04-25', 4000000, '월~금 09:30~18:00', '서울특별시 금천구 가산디지털1로 70', true, true, true, 'JavaScript/HTML 기초 이상', 1450, 'APPROVED', NOW(), NOW(), NOW(), false);

INSERT INTO course (id, academy_id, requester_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, view_count, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (9, 3, 15, 8, 'React + Next.js 현업 프로젝트', '2025-11-12', '2025-12-08', '2025-12-15', '2026-03-20', 3300000, '화목 19:00~22:30', '온라인', false, false, false, 'React 기본 경험', 950, 'APPROVED', NOW(), NOW(), NOW(), false);

INSERT INTO course (id, academy_id, requester_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, view_count, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (10, 1, 13, 7, 'Spring + Kotlin 마이크로서비스', '2025-12-05', '2025-12-25', '2026-01-03', '2026-05-15', 4200000, '금 19:00~23:00', '온라인', false, true, false, 'Spring Boot 실무 경험', 780, 'APPROVED', NOW(), NOW(), NOW(), false);

INSERT INTO course (id, academy_id, requester_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, view_count, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (11, 2, 14, 11, '클라우드 보안 운영 마스터', '2025-11-02', '2025-11-20', '2025-12-01', '2026-03-01', 3600000, '수금 19:00~22:30', '서울특별시 금천구 가산디지털1로 70', true, false, true, '클라우드 기본 지식', 640, 'APPROVED', NOW(), NOW(), NOW(), false);

INSERT INTO course (id, academy_id, requester_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, view_count, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (12, 3, 15, 9, '데이터 엔지니어링 자동화', '2025-11-15', '2025-12-10', '2026-01-05', '2026-04-25', 3900000, '월수금 18:30~22:00', '경기도 성남시 성남대로 34', false, true, true, 'SQL/파이썬 기본', 710, 'APPROVED', NOW(), NOW(), NOW(), false);

INSERT INTO course (id, academy_id, requester_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, view_count, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (13, 1, 13, 3, 'AI 챗봇 실무 구축 트랙', '2025-12-12', '2025-12-29', '2026-01-07', '2026-04-07', 3400000, '주말 09:00~17:00', '온라인', false, false, false, 'Python/REST 기본', 520, 'APPROVED', NOW(), NOW(), NOW(), false);

INSERT INTO course (id, academy_id, requester_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, view_count, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (14, 2, 14, 10, 'DevOps와 GitOps 실무', '2025-11-18', '2025-12-08', '2025-12-16', '2026-03-31', 3100000, '월수 20:00~23:00', '온라인', true, true, false, '리눅스/스크립트 기반', 465, 'APPROVED', NOW(), NOW(), NOW(), false);

INSERT INTO course (id, academy_id, requester_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, view_count, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (15, 3, 15, 1, '웹개발 취업 집중 트랙', '2025-11-05', '2025-12-05', '2025-12-12', '2026-04-10', 3600000, '월~금 09:00~18:00', '경기도 성남시 성남대로 34', true, false, true, '기초 알고리즘 숙련', 1010, 'APPROVED', NOW(), NOW(), NOW(), false);

INSERT INTO course (id, academy_id, requester_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, view_count, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (16, 1, 13, 9, '데이터 시각화 & BI 전문가 과정', '2025-10-28', '2025-11-24', '2025-11-30', '2026-03-31', 3600000, '화목 19:00~22:00', '온라인', false, true, false, 'SQL 및 Tableau/Power BI 기초', 530, 'APPROVED', NOW(), NOW(), NOW(), false);

INSERT INTO course (id, academy_id, requester_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, view_count, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (17, 2, 14, 12, '사이버보안 취약점 실습 트랙', '2025-12-07', '2025-12-28', '2026-01-09', '2026-04-20', 3700000, '금 18:30~22:00', '서울특별시 금천구 가산디지털1로 70', false, false, true, '네트워크 및 Linux 경험', 410, 'APPROVED', NOW(), NOW(), NOW(), false);

INSERT INTO course (id, academy_id, requester_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, view_count, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (18, 3, 15, 11, '클라우드 마이그레이션 실무', '2025-11-22', '2025-12-20', '2026-01-08', '2026-04-30', 4100000, '월수금 18:30~22:30', '경기도 성남시 성남대로 34', true, true, true, 'AWS/GCP 기초', 380, 'APPROVED', NOW(), NOW(), NOW(), false);

-- 5. CourseReview (강의별 0~5개 리뷰)
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (1, 3, 1, '엔터프라이즈 구조와 설계를 동시에 다룬 구성이라 이해도가 빨라졌습니다. 실습이 풍부합니다.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (2, 4, 1, '강사님이 실제 프로젝트에서 사용하는 패턴을 설명해주셔서 현업 적응에 도움이 되었어요.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (3, 5, 1, '리팩터링 사례가 특히 기억에 남습니다. 아키텍처 챕터를 끝까지 들으면 완성도 높습니다.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (4, 6, 1, '실습 서버 환경이 조금 느렸지만 배포 파트는 디테일하게 다뤄집니다.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (5, 7, 2, '프론트엔드 설계 흐름을 전체적으로 짚어줘서 구조를 잡는 데 도움됐습니다.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (6, 8, 2, '진짜 실전에서 쓰는 Storybook/Testing을 배워서 자신감이 생겼어요.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (7, 9, 2, '모노리포와 워크스페이스를 다룰 때 기준을 잡게 해줘서 팀 협업에 도움이 되었습니다.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (8, 10, 3, '데이터 워크플로우를 단계별로 실습할 수 있어서 높은 몰입감을 느꼈습니다.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (9, 11, 3, '전처리부터 ML 배포까지 한 번에 확인할 수 있고, Python 자료도 풍부합니다.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (10, 12, 3, '대량 데이터 처리 챕터를 통해 현업에서 쓰는 기법을 실제로 세팅해볼 수 있었어요.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (11, 7, 3, '챕터 간 연결이 매끄럽고 멘토 리뷰도 직접 받았습니다.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (12, 8, 4, '클라우드 설계 개념 이해에 도움이 되었습니다. 실습이 많아 시간은 꽤 들었어요.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (13, 9, 4, 'AWS 디자인 패턴을 실제 아키텍처로 풀어낸 부분이 특히 인상적이었습니다.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (14, 10, 5, '실습 위주라 실무 감각을 빠르게 익힐 수 있습니다.', 'APPROVED', 'JOB_SEEKER', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (15, 11, 6, 'AI 모델 커스터마이징이 많아 기존 지식만으로도 따라갈 수 있었어요.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (16, 12, 6, '연구자료를 활용한 실습이 많아서 깊이가 있습니다.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (17, 3, 6, '릴리스 자동화와 모델 서빙을 도전해볼 수 있어 좋았어요.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (18, 4, 7, '보안 실습을 통해 제가 놓쳤던 부분들을 다시 점검했습니다.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (19, 5, 8, '풀스택을 처음 시작하는 분이라면 이 과정을 먼저 들으면 방향이 잡힙니다.', 'APPROVED', 'JOB_SEEKER', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (20, 6, 8, '대형 프로젝트를 2개나 진행할 수 있어서 포트폴리오에 바로 활용했습니다.', 'APPROVED', 'JOB_SEEKER', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (21, 7, 8, '학습 커뮤니케이션이 활발하고 팀 매칭도 실무와 유사하게 구성됩니다.', 'APPROVED', 'JOB_SEEKER', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (22, 8, 9, 'Next.js 및 Vercel 배포를 반복해서 연습할 수 있어서 자신감이 생깁니다.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (23, 9, 9, 'SSR/SSG 차이를 체감하며 설계할 기회를 줍니다.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (24, 10, 10, 'Kotlin+Spring 세팅이 특히 정리되어 있어서 라이브러리 의존성을 이해하기 좋았습니다.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (25, 11, 10, '다양한 모니터링/로깅 설정까지 다뤄져 있어 안정적인 서비스를 만들 수 있습니다.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (26, 12, 10, '조금 빠르게 진행되지만 Replay영상을 다시 보면 충분히 따라갈 수 있어요.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (27, 3, 11, '클라우드 보안 정책을 실제 템플릿으로 만들어보며 체득했습니다.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (28, 4, 11, 'IAM/네트워크 구성 실습이 타이트해서 집중력 있게 들었습니다.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (29, 5, 13, '챗봇 운영사례를 직접 구현해보며 아이디어를 얻었습니다.', 'APPROVED', 'JOB_SEEKER', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (30, 6, 13, 'Dialogflow와 FastAPI 연동 구조가 실무에서 바로 적용 가능한 내용이에요.', 'APPROVED', 'JOB_SEEKER', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (31, 7, 14, '인프라 코드와 GitOps를 연동하는 흐름이 명확합니다.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (32, 8, 14, 'Flux CD 실습으로 운영 프로젝트에 바로 적용했습니다.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (33, 9, 14, 'Terraform 모듈을 설계하면서 표준화를 체득하게 됩니다.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (34, 10, 14, 'CDN과 Terraform을 동시에 다루는 프로젝트가 인상적입니다.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (35, 11, 14, '애자일 스프린트 기반으로 실습해서 실제 팀워크를 경험했습니다.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (36, 12, 15, '취업 준비를 함께하는 멘토링이 잘 짜여져 있습니다.', 'APPROVED', 'JOB_SEEKER', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (37, 3, 15, '이력서/포트폴리오 코칭 덕분에 서류 합격률이 높아졌어요.', 'APPROVED', 'JOB_SEEKER', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (38, 4, 15, '사전 학습 자료가 풍부해서 예습 없이도 시작할 수 있습니다.', 'APPROVED', 'JOB_SEEKER', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (39, 5, 16, '시각화 도구 활용법을 직접 따라 하며 습득할 수 있었습니다.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (40, 6, 16, '실제 BI 리포트 케이스로 방향을 잡기 좋습니다.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (41, 7, 17, '취약점 분석 실습을 반복할 수 있어서 자신감이 붙었습니다.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (42, 8, 17, '실습 랩 환경이 철저히 보호되어 있으며 대비책을 실습으로 익힙니다.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (43, 9, 17, 'Red Team/Blue Team을 오가는 실습이 흥미로웠습니다.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (44, 10, 17, '공격/방어 시나리오를 통해 대응력을 기릅니다.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, type, created_at, updated_at, is_deleted)
VALUES (45, 11, 18, '마이그레이션 체크리스트를 실습하며 구성할 수 있도록 구성되어 있습니다.', 'APPROVED', 'EMPLOYEE', NOW(), NOW(), false);

-- 6. ReviewSection (선택된 리뷰에 대한 세부 점수)
INSERT INTO review_section (review_id, section_type, score, comment, created_at, updated_at, is_deleted)
VALUES (1, 'CURRICULUM', 5, '커리큘럼이 실무 흐름을 잘 따라갑니다.', NOW(), NOW(), false);
INSERT INTO review_section (review_id, section_type, score, comment, created_at, updated_at, is_deleted)
VALUES (1, 'COURSEWARE', 4, '자료가 새로 업데이트되어 있습니다.', NOW(), NOW(), false);
INSERT INTO review_section (review_id, section_type, score, comment, created_at, updated_at, is_deleted)
VALUES (1, 'INSTRUCTOR', 5, '강사님 경험이 풍부하여 질문에 빠르게 답변해주십니다.', NOW(), NOW(), false);
INSERT INTO review_section (review_id, section_type, score, comment, created_at, updated_at, is_deleted)
VALUES (1, 'EQUIPMENT', 4, '실습 환경이 깔끔합니다.', NOW(), NOW(), false);
INSERT INTO review_section (review_id, section_type, score, comment, created_at, updated_at, is_deleted)
VALUES (2, 'CURRICULUM', 5, '아키텍처 품질 관련 챕터가 가장 좋았습니다.', NOW(), NOW(), false);
INSERT INTO review_section (review_id, section_type, score, comment, created_at, updated_at, is_deleted)
VALUES (2, 'INSTRUCTOR', 5, '현업 사례가 풍부합니다.', NOW(), NOW(), false);
INSERT INTO review_section (review_id, section_type, score, comment, created_at, updated_at, is_deleted)
VALUES (3, 'COURSEWARE', 5, '연습용 템플릿 파일이 공유되어 유용합니다.', NOW(), NOW(), false);
INSERT INTO review_section (review_id, section_type, score, comment, created_at, updated_at, is_deleted)
VALUES (4, 'EQUIPMENT', 3, '클라우드 실습 서버가 가끔 지연됩니다.', NOW(), NOW(), false);

-- 7. CourseQna (Q&A 샘플)
INSERT INTO course_qna (course_id, account_id, answered_by_id, title, question_text, answer_text, is_answered, created_at, updated_at, is_deleted, deleted_at)
VALUES (1, 3, 2, 'Spring Data와 Querydsl 활용 가능한가요?', 'Querydsl 스니펫도 같이 제공되는지 궁금합니다.', 'Querydsl은 실습 챕터에서 샘플 코드를 제공합니다. 강의자료 내 링크로 바로 내려받으세요.', true, NOW(), NOW(), false, NULL);
INSERT INTO course_qna (course_id, account_id, answered_by_id, title, question_text, answer_text, is_answered, created_at, updated_at, is_deleted, deleted_at)
VALUES (8, 5, 2, '팀 매칭은 어떻게 되나요?', '팀 매칭 기준이 궁금합니다. 미리 팀원을 지정할 수 있나요?', '팀 매칭은 중간 평가 이후 성향 기반으로 자동 배정됩니다. 원하는 팀원이 있다면 운영담당자에게 미리 알려주세요.', true, NOW(), NOW(), false, NULL);
INSERT INTO course_qna (course_id, account_id, answered_by_id, title, question_text, answer_text, is_answered, created_at, updated_at, is_deleted, deleted_at)
VALUES (9, 6, NULL, 'Next.js와 React를 혼합할 수 있나요?', 'Next.js 프로젝트에서 기존 React 코드를 함께 사용할 수 있는지 궁금합니다.', NULL, false, NOW(), NOW(), false, NULL);
INSERT INTO course_qna (course_id, account_id, answered_by_id, title, question_text, answer_text, is_answered, created_at, updated_at, is_deleted, deleted_at)
VALUES (11, 7, 14, '대면 수업 참석률은 어떻게 체크하나요?', '출결 체크 방식이 궁금합니다.', '분당 캠퍼스는 출결 QR과 LMS 체크인을 병행합니다.', true, NOW(), NOW(), false, NULL);
INSERT INTO course_qna (course_id, account_id, answered_by_id, title, question_text, answer_text, is_answered, created_at, updated_at, is_deleted, deleted_at)
VALUES (15, 8, NULL, '취업 연계 혜택은 무엇이 있나요?', '수료 후 취업 지원 프로그램이 있는지 궁금합니다.', NULL, false, NOW(), NOW(), false, NULL);
INSERT INTO course_qna (course_id, account_id, answered_by_id, title, question_text, answer_text, is_answered, created_at, updated_at, is_deleted, deleted_at)
VALUES (17, 9, 15, '취약점 랩 환경에서 실습 로그는 남나요?', '실습 기록이 저장되는지, 피드백을 받을 수 있는지 궁금합니다.', '운영팀이 실습 로그를 별도 관리하고 피드백을 남겨드립니다.', true, NOW(), NOW(), false, NULL);

-- 8. Board (커뮤니티 게시글 30개)
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (1, 'NOTICE', '2026년 1월 강의 일정 안내', '2026년 1월 재직자 및 취업예정자 과정 일정이 확정되었습니다. 과정 페이지에서 상세 스케줄을 확인하세요.', 340, false, 2, NOW(), NOW(), false, NULL);
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (2, 'NOTICE', '겨울방학 온라인 특강 안내', '12월부터 1월까지 온라인 무료 특강을 진행합니다. 선착순 100명으로 마감됩니다.', 210, false, 2, NOW(), NOW(), false, NULL);
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (3, 'NOTICE', '신규 데이터 엔지니어링 과정 오픈', '데이터 엔지니어링 자동화 과정은 실습 중심으로 구성되어 있으며 2026년 3월 개강합니다.', 180, false, 3, NOW(), NOW(), false, NULL);
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (4, 'NOTICE', 'KOSTA 가산 캠퍼스 방역 안내', '가산 캠퍼스 방문 시 QR 체크인과 마스크 착용을 부탁드립니다.', 125, false, 2, NOW(), NOW(), false, NULL);
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (5, 'NOTICE', '분당 캠퍼스 2025년 우수 수강생 발표', '우수 수강생 대상 무상 멘토링과 취업 컨설팅을 제공합니다.', 98, false, 15, NOW(), NOW(), false, NULL);
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (6, 'NOTICE', '국비 지원 서류 접수 마감 안내', '11월 30일 접수가 마감되며 대기자는 다음 기수로 자동 이월됩니다.', 210, false, 14, NOW(), NOW(), false, NULL);
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (7, 'NOTICE', '동계특화 멘토링 모집', 'AI/DevOps 실무 멘토링을 6주 동안 진행하며 신청을 받습니다.', 150, false, 13, NOW(), NOW(), false, NULL);
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (8, 'NOTICE', '서비스 이용 시간 안내', '상담센터는 평일 9시부터 18시까지 운영됩니다.', 88, false, 2, NOW(), NOW(), false, NULL);
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (9, 'QUESTION', '기수별 수강료 분할 납부 문의', '분기별로 수강료를 나눠 납부할 수 있나요? 관련 서류가 필요한지도 알고 싶습니다.', 76, false, 3, NOW(), NOW(), false, NULL);
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (10, 'QUESTION', '온라인 실습 환경 브라우저 문의', 'Chrome에서 VSCode 서버 연결이 잘 안 되는데 특별한 설정이 필요한가요?', 64, false, 4, NOW(), NOW(), false, NULL);
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (11, 'QUESTION', '국비 수강생 서류 제출 일정 문의', 'HRD-Net 서류 제출 시 먼저 어떤 문서를 제출해야 하나요?', 59, true, 5, NOW(), NOW(), false, NULL);
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (12, 'QUESTION', '과정 취소 후 환불 정책', '개강 이후 환불 기준과 절차가 궁금합니다.', 88, false, 6, NOW(), NOW(), false, NULL);
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (13, 'QUESTION', '오프라인 과정 주차 안내', '가산 캠퍼스 인근 주차 공간이 있나요? 대중교통 외에도 접근성이 궁금합니다.', 73, false, 7, NOW(), NOW(), false, NULL);
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (14, 'QUESTION', '수강 인증서 발급 문의', '수료증 PDF 다운로드는 어떻게 하나요?', 62, false, 8, NOW(), NOW(), false, NULL);
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (15, 'QUESTION', '모바일 과정 교재 제공 여부', '모바일 앱 과정 교재는 인쇄본으로 제공되나요?', 55, false, 9, NOW(), NOW(), false, NULL);
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (16, 'QUESTION', '취업 연계 기업 면접 일정', '수료생 대상 면접은 어느 시기에 시작되나요?', 48, true, 10, NOW(), NOW(), false, NULL);
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (17, 'COURSE_STORY', 'AI 현업 전환 지원 후기', 'AI R&D 과정을 수료하고 데이터 팀으로 이직했습니다. 실습 중심 커리큘럼이 큰 도움이었습니다.', 210, false, 11, NOW(), NOW(), false, NULL);
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (18, 'COURSE_STORY', '비전공자 프론트 전향기', 'HTML부터 시작해 Next.js까지 경험하면서 프론트엔드 실무로 전향했습니다.', 180, false, 12, NOW(), NOW(), false, NULL);
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (19, 'COURSE_STORY', '백엔드 경력 보강 후기', 'Spring + Kotlin 과정으로 안정적 코드 리뷰 루틴을 갖추었습니다.', 160, false, 3, NOW(), NOW(), false, NULL);
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (20, 'COURSE_STORY', '클라우드 운영팀 합류 이야기', '클라우드 마이그레이션 트랙 이후 운영팀에 합류하면서 직무 변화에 성공했습니다.', 190, false, 4, NOW(), NOW(), false, NULL);
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (21, 'COURSE_STORY', '보안 트레이닝 수료기', '취약점 실습을 통해 사고 대응 속도가 빨라졌습니다.', 135, false, 5, NOW(), NOW(), false, NULL);
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (22, 'COURSE_STORY', '풀스택 부트캠프 도전기', '매일 실습으로 포트폴리오를 완성하고 기술 면접을 준비했습니다.', 142, false, 6, NOW(), NOW(), false, NULL);
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (23, 'COURSE_STORY', 'DevOps 도입 경험담', 'GitOps와 모니터링을 적용하며 팀에 안정성을 가져왔습니다.', 155, false, 7, NOW(), NOW(), false, NULL);
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (24, 'COURSE_STORY', '데이터 사이언티스트 전환 후기', '시각화 과정과 프로젝트가 경력 전환에 결정적 도움이 되었습니다.', 170, false, 8, NOW(), NOW(), false, NULL);
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (25, 'CODING_STORY', 'Spring Boot 배포 자동화 팁', 'Gradle과 Docker를 이용해 배포 자동화를 구성한 경험을 공유합니다.', 182, false, 9, NOW(), NOW(), false, NULL);
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (26, 'CODING_STORY', 'React 상태관리 비교', 'Zustand와 Redux Toolkit을 실험하며 장단점을 정리했습니다.', 144, false, 10, NOW(), NOW(), false, NULL);
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (27, 'CODING_STORY', '알고리즘 스터디 루틴 공개', '매일 2문제씩 풀고 리뷰하는 루틴을 꾸준히 지키면 실력이 오른다는 경험담입니다.', 130, false, 11, NOW(), NOW(), false, NULL);
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (28, 'CODING_STORY', 'TypeScript 대용량 코드 베이스 경험', '대규모 프로젝트에 TypeScript를 적용한 사례를 정리했습니다.', 150, false, 12, NOW(), NOW(), false, NULL);
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (29, 'CODING_STORY', '클라우드 CI/CD 설계', 'GitHub Actions와 Cloud Run을 결합해 자동 배포 파이프라인을 만들었습니다.', 165, false, 3, NOW(), NOW(), false, NULL);
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted, deleted_at)
VALUES (30, 'CODING_STORY', '보안 테스트 자동화', 'OWASP ZAP과 Selenium을 활용한 테스트 자동화를 소개합니다.', 142, false, 4, NOW(), NOW(), false, NULL);

-- 9. Comment (게시글 댓글 20개)
INSERT INTO comment (id, board_id, account_id, comment_id, text, is_secret, created_at, updated_at, is_deleted, deleted_at)
VALUES (1, 1, 3, NULL, '일정 확인했습니다. 감사합니다!', false, NOW(), NOW(), false, NULL);
INSERT INTO comment (id, board_id, account_id, comment_id, text, is_secret, created_at, updated_at, is_deleted, deleted_at)
VALUES (2, 1, 4, 1, '저도 일정 맞춰서 신청하려고요.', false, NOW(), NOW(), false, NULL);
INSERT INTO comment (id, board_id, account_id, comment_id, text, is_secret, created_at, updated_at, is_deleted, deleted_at)
VALUES (3, 2, 5, NULL, '무료 특강 신청은 어디서 하나요?', false, NOW(), NOW(), false, NULL);
INSERT INTO comment (id, board_id, account_id, comment_id, text, is_secret, created_at, updated_at, is_deleted, deleted_at)
VALUES (4, 2, 2, 3, '공지 하단 링크에서 신청 가능합니다.', false, NOW(), NOW(), false, NULL);
INSERT INTO comment (id, board_id, account_id, comment_id, text, is_secret, created_at, updated_at, is_deleted, deleted_at)
VALUES (5, 9, 2, NULL, '분할 납부는 2회 분할까지 가능하며 별도 서류 없이 신청할 수 있습니다.', false, NOW(), NOW(), false, NULL);
INSERT INTO comment (id, board_id, account_id, comment_id, text, is_secret, created_at, updated_at, is_deleted, deleted_at)
VALUES (6, 10, 13, NULL, 'Chrome 최신 버전으로 업데이트 후 캐시 삭제를 권장드립니다.', false, NOW(), NOW(), false, NULL);
INSERT INTO comment (id, board_id, account_id, comment_id, text, is_secret, created_at, updated_at, is_deleted, deleted_at)
VALUES (7, 12, 14, NULL, '환불 규정은 마이페이지 > 수강 내역에서 확인 가능합니다.', false, NOW(), NOW(), false, NULL);
INSERT INTO comment (id, board_id, account_id, comment_id, text, is_secret, created_at, updated_at, is_deleted, deleted_at)
VALUES (8, 13, 14, NULL, '가산 캠퍼스 인근 공영주차장을 이용하시면 됩니다.', false, NOW(), NOW(), false, NULL);
INSERT INTO comment (id, board_id, account_id, comment_id, text, is_secret, created_at, updated_at, is_deleted, deleted_at)
VALUES (9, 17, 6, NULL, '저도 AI 과정 덕분에 취업했어요. 정말 추천합니다.', false, NOW(), NOW(), false, NULL);
INSERT INTO comment (id, board_id, account_id, comment_id, text, is_secret, created_at, updated_at, is_deleted, deleted_at)
VALUES (10, 18, 7, NULL, '비전공자로서 공감되는 후기네요. 힘이 됩니다!', false, NOW(), NOW(), false, NULL);
INSERT INTO comment (id, board_id, account_id, comment_id, text, is_secret, created_at, updated_at, is_deleted, deleted_at)
VALUES (11, 19, 8, NULL, 'Kotlin 코루틴 부분이 특히 좋았어요.', false, NOW(), NOW(), false, NULL);
INSERT INTO comment (id, board_id, account_id, comment_id, text, is_secret, created_at, updated_at, is_deleted, deleted_at)
VALUES (12, 20, 9, NULL, '클라우드 과정 저도 듣고 싶네요.', false, NOW(), NOW(), false, NULL);
INSERT INTO comment (id, board_id, account_id, comment_id, text, is_secret, created_at, updated_at, is_deleted, deleted_at)
VALUES (13, 25, 10, NULL, 'Gradle + Docker 조합 정리 감사합니다!', false, NOW(), NOW(), false, NULL);
INSERT INTO comment (id, board_id, account_id, comment_id, text, is_secret, created_at, updated_at, is_deleted, deleted_at)
VALUES (14, 26, 11, NULL, 'Zustand가 가벼워서 좋더라고요.', false, NOW(), NOW(), false, NULL);
INSERT INTO comment (id, board_id, account_id, comment_id, text, is_secret, created_at, updated_at, is_deleted, deleted_at)
VALUES (15, 26, 12, 14, '저는 Redux Toolkit 쪽이 더 익숙해요.', false, NOW(), NOW(), false, NULL);
INSERT INTO comment (id, board_id, account_id, comment_id, text, is_secret, created_at, updated_at, is_deleted, deleted_at)
VALUES (16, 27, 3, NULL, '알고리즘 스터디 같이 하실 분 계신가요?', false, NOW(), NOW(), false, NULL);
INSERT INTO comment (id, board_id, account_id, comment_id, text, is_secret, created_at, updated_at, is_deleted, deleted_at)
VALUES (17, 27, 4, 16, '저요! DM 주세요.', false, NOW(), NOW(), false, NULL);
INSERT INTO comment (id, board_id, account_id, comment_id, text, is_secret, created_at, updated_at, is_deleted, deleted_at)
VALUES (18, 29, 5, NULL, 'GitHub Actions 워크플로우 예제 공유 감사합니다.', false, NOW(), NOW(), false, NULL);
INSERT INTO comment (id, board_id, account_id, comment_id, text, is_secret, created_at, updated_at, is_deleted, deleted_at)
VALUES (19, 30, 6, NULL, 'OWASP ZAP 설정이 까다로웠는데 도움 됐어요.', false, NOW(), NOW(), false, NULL);
INSERT INTO comment (id, board_id, account_id, comment_id, text, is_secret, created_at, updated_at, is_deleted, deleted_at)
VALUES (20, 30, 7, 19, '저도 같은 부분에서 헤맸었어요.', false, NOW(), NOW(), false, NULL);

-- End of mock data