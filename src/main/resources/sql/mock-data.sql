-- --------------------------------------------------------------------
-- Mock Data for Software Campus
-- 작성일자: 2025-11-26
-- --------------------------------------------------------------------

-- 1. Academy (기관 데이터)
INSERT INTO academy (id, name, address, business_number, email, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (1, 'KOSTA 종로', '서울특별시 종로구 우정국로 2길 21 대왕빌딩 7층', '031-606-9321', 'jongno@kosta.com', 'APPROVED', NOW(), NOW(), NOW(), false);

INSERT INTO academy (id, name, address, business_number, email, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (2, 'KOSTA 가산', '서울특별시 금천구 가산디지털1로 70 호서대벤처타워 9층', '02-6278-9357', 'gasan@kosta.com', 'APPROVED', NOW(), NOW(), NOW(), false);

INSERT INTO academy (id, name, address, business_number, email, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (3, 'KOSTA 분당', '경기도 성남시 성남대로 34 하나플라자 6층', '031-606-9311', 'bundang@kosta.com', 'APPROVED', NOW(), NOW(), NOW(), false);


-- 2. CourseCategory (과정 카테고리 데이터)
-- 채용예정자 (JOB_SEEKER)
INSERT INTO course_category (id, category_type, category_name, created_at, updated_at, is_deleted) VALUES (1, 'JOB_SEEKER', '웹개발', NOW(), NOW(), false);
INSERT INTO course_category (id, category_type, category_name, created_at, updated_at, is_deleted) VALUES (2, 'JOB_SEEKER', '모바일', NOW(), NOW(), false);
INSERT INTO course_category (id, category_type, category_name, created_at, updated_at, is_deleted) VALUES (3, 'JOB_SEEKER', '데이터/AI', NOW(), NOW(), false);
INSERT INTO course_category (id, category_type, category_name, created_at, updated_at, is_deleted) VALUES (4, 'JOB_SEEKER', '클라우드', NOW(), NOW(), false);
INSERT INTO course_category (id, category_type, category_name, created_at, updated_at, is_deleted) VALUES (5, 'JOB_SEEKER', '보안', NOW(), NOW(), false);
INSERT INTO course_category (id, category_type, category_name, created_at, updated_at, is_deleted) VALUES (6, 'JOB_SEEKER', 'IoT/임베디드', NOW(), NOW(), false);
INSERT INTO course_category (id, category_type, category_name, created_at, updated_at, is_deleted) VALUES (7, 'JOB_SEEKER', '게임/블록체인', NOW(), NOW(), false);

-- 재직자 (EMPLOYEE)
INSERT INTO course_category (id, category_type, category_name, created_at, updated_at, is_deleted) VALUES (8, 'EMPLOYEE', 'Infra운영', NOW(), NOW(), false);
INSERT INTO course_category (id, category_type, category_name, created_at, updated_at, is_deleted) VALUES (9, 'EMPLOYEE', '백엔드 개발', NOW(), NOW(), false);
INSERT INTO course_category (id, category_type, category_name, created_at, updated_at, is_deleted) VALUES (10, 'EMPLOYEE', '프론트엔드 개발', NOW(), NOW(), false);
INSERT INTO course_category (id, category_type, category_name, created_at, updated_at, is_deleted) VALUES (11, 'EMPLOYEE', 'DB', NOW(), NOW(), false);
INSERT INTO course_category (id, category_type, category_name, created_at, updated_at, is_deleted) VALUES (12, 'EMPLOYEE', 'AI', NOW(), NOW(), false);
INSERT INTO course_category (id, category_type, category_name, created_at, updated_at, is_deleted) VALUES (13, 'EMPLOYEE', 'SW요구분석', NOW(), NOW(), false);
INSERT INTO course_category (id, category_type, category_name, created_at, updated_at, is_deleted) VALUES (14, 'EMPLOYEE', '백엔드개발자', NOW(), NOW(), false);
INSERT INTO course_category (id, category_type, category_name, created_at, updated_at, is_deleted) VALUES (15, 'EMPLOYEE', '클라우드엔지니어', NOW(), NOW(), false);
INSERT INTO course_category (id, category_type, category_name, created_at, updated_at, is_deleted) VALUES (16, 'EMPLOYEE', '프론트엔드개발자', NOW(), NOW(), false);
INSERT INTO course_category (id, category_type, category_name, created_at, updated_at, is_deleted) VALUES (17, 'EMPLOYEE', '데이터엔지니어', NOW(), NOW(), false);
INSERT INTO course_category (id, category_type, category_name, created_at, updated_at, is_deleted) VALUES (18, 'EMPLOYEE', 'AI엔지니어', NOW(), NOW(), false);
INSERT INTO course_category (id, category_type, category_name, created_at, updated_at, is_deleted) VALUES (19, 'EMPLOYEE', 'SW아키텍트', NOW(), NOW(), false);
INSERT INTO course_category (id, category_type, category_name, created_at, updated_at, is_deleted) VALUES (20, 'EMPLOYEE', '정보보안전문가', NOW(), NOW(), false);
INSERT INTO course_category (id, category_type, category_name, created_at, updated_at, is_deleted) VALUES (21, 'EMPLOYEE', 'IT기획자/컨설턴트', NOW(), NOW(), false);
INSERT INTO course_category (id, category_type, category_name, created_at, updated_at, is_deleted) VALUES (22, 'EMPLOYEE', '데이터분석가', NOW(), NOW(), false);
INSERT INTO course_category (id, category_type, category_name, created_at, updated_at, is_deleted) VALUES (23, 'EMPLOYEE', '비즈니스분석가', NOW(), NOW(), false);
INSERT INTO course_category (id, category_type, category_name, created_at, updated_at, is_deleted) VALUES (24, 'EMPLOYEE', '데이터사이언티스트', NOW(), NOW(), false);


-- 3. Account (사용자 데이터)
-- 관리자 계정
-- 비밀번호: test (BCrypt 해시)
INSERT INTO account (id, user_name, password, email, phone_number, account_type, affiliation, position, address, account_approved, academy_id, created_at, updated_at, is_deleted)
VALUES (1, '관리자', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin@test.com', '010-1111-1111', 'ADMIN', NULL, NULL, NULL, 'APPROVED', NULL, NOW(), NOW(), false);

-- 일반 사용자 계정 (비밀번호: test)
INSERT INTO account (id, user_name, password, email, phone_number, account_type, affiliation, position, address, account_approved, academy_id, created_at, updated_at, is_deleted)
VALUES (2, '일반사용자', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'user@test.com', '010-2222-2222', 'USER', NULL, NULL, '서울시 서초구', 'APPROVED', NULL, NOW(), NOW(), false);

-- 기관 사용자 계정 (KOSTA 종로 소속, 비밀번호: test)
INSERT INTO account (id, user_name, password, email, phone_number, account_type, affiliation, position, address, account_approved, academy_id, created_at, updated_at, is_deleted)
VALUES (3, '소프트캠퍼스 담당자', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'academy@test.com', '010-3333-3333', 'ACADEMY', '소프트캠퍼스', '교육팀장', '서울시 종로구', 'APPROVED', 1, NOW(), NOW(), false);

-- 추가 일반 사용자 (리뷰/댓글 작성용)
INSERT INTO account (id, user_name, password, email, phone_number, account_type, affiliation, position, address, account_approved, academy_id, created_at, updated_at, is_deleted)
VALUES (4, '김철수', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'kim@test.com', '010-4444-4444', 'USER', NULL, NULL, '서울시 강남구', 'APPROVED', NULL, NOW(), NOW(), false);

INSERT INTO account (id, user_name, password, email, phone_number, account_type, affiliation, position, address, account_approved, academy_id, created_at, updated_at, is_deleted)
VALUES (5, '이영희', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'lee@test.com', '010-5555-5555', 'USER', NULL, NULL, '서울시 송파구', 'APPROVED', NULL, NOW(), NOW(), false);

INSERT INTO account (id, user_name, password, email, phone_number, account_type, affiliation, position, address, account_approved, academy_id, created_at, updated_at, is_deleted)
VALUES (6, '박민수', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'park@test.com', '010-6666-6666', 'USER', NULL, NULL, '서울시 마포구', 'APPROVED', NULL, NOW(), NOW(), false);


-- 4. Course (과정 데이터)
-- 재직자 과정 (EMPLOYEE)
-- 1. Spring Boot 백엔드 개발 (온라인) - KOSTA 종로, 백엔드개발자(14번)
INSERT INTO course (id, academy_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (1, 1, 14, 'Spring Boot 백엔드 개발', '2025-01-01', '2025-01-31', '2025-02-01', '2025-05-31', 0, '화목 19:00~22:00', '온라인', false, true, false, 'Java 기초', 'APPROVED', NOW(), NOW(), NOW(), false);

-- 2. React 프론트엔드 실무 (온라인) - KOSTA 가산, 프론트엔드개발자(16번)
INSERT INTO course (id, academy_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (2, 2, 16, 'React 프론트엔드 실무', '2025-01-15', '2025-02-14', '2025-02-15', '2025-05-15', 0, '월수금 20:00~22:00', '온라인', false, false, false, 'JavaScript 기초', 'APPROVED', NOW(), NOW(), NOW(), false);

-- 3. 데이터 엔지니어링 실무 (오프라인) - KOSTA 분당, 데이터엔지니어(17번)
INSERT INTO course (id, academy_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (3, 3, 17, '데이터 엔지니어링 실무', '2025-02-01', '2025-02-28', '2025-03-01', '2025-06-30', 0, '평일 19:30~22:30', '경기도 성남시 성남대로 34 하나플라자 6층', false, false, true, 'Python 기초', 'APPROVED', NOW(), NOW(), NOW(), false);

-- 4. 클라우드 아키텍처 설계 (온라인) - KOSTA 종로, 클라우드엔지니어(15번)
INSERT INTO course (id, academy_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (4, 1, 15, '클라우드 아키텍처 설계', '2025-01-10', '2025-02-09', '2025-02-10', '2025-05-10', 0, '화목 20:00~22:00', '온라인', false, true, false, '리눅스 기초', 'APPROVED', NOW(), NOW(), NOW(), false);

-- 5. DevOps 엔지니어 양성 (온라인) - KOSTA 가산, Infra운영(8번)
INSERT INTO course (id, academy_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (5, 2, 8, 'DevOps 엔지니어 양성', '2025-01-20', '2025-02-19', '2025-02-20', '2025-05-20', 0, '수금 20:00~22:30', '온라인', false, false, false, '없음', 'APPROVED', NOW(), NOW(), NOW(), false);

-- 6. AI/ML 실무 적용 (온라인) - KOSTA 분당, AI엔지니어(18번)
INSERT INTO course (id, academy_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (6, 3, 18, 'AI/ML 실무 적용', '2025-02-01', '2025-03-01', '2025-03-02', '2025-06-30', 0, '월수금 19:30~22:00', '온라인', false, true, false, 'Python 중급', 'APPROVED', NOW(), NOW(), NOW(), false);


-- 취업예정자 과정 (JOB_SEEKER)
-- 7. 풀스택 개발자 부트캠프 (오프라인) - KOSTA 종로, 웹개발(1번)
INSERT INTO course (id, academy_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (7, 1, 1, '풀스택 개발자 부트캠프', '2025-01-01', '2025-01-31', '2025-02-01', '2025-07-31', 0, '월~금 09:00~18:00', '서울특별시 종로구 우정국로 2길 21 대왕빌딩 7층', true, true, true, '없음', 'APPROVED', NOW(), NOW(), NOW(), false);

-- 8. 프론트엔드 개발자 양성 (오프라인) - KOSTA 가산, 웹개발(1번)
INSERT INTO course (id, academy_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (8, 2, 1, '프론트엔드 개발자 양성', '2025-01-15', '2025-02-14', '2025-02-15', '2025-08-14', 0, '월~금 09:00~18:00', '서울특별시 금천구 가산디지털1로 70 호서대벤처타워 9층', true, true, true, '없음', 'APPROVED', NOW(), NOW(), NOW(), false);

-- 9. 백엔드 개발자 취업 과정 (오프라인) - KOSTA 분당, 웹개발(1번)
INSERT INTO course (id, academy_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (9, 3, 1, '백엔드 개발자 취업 과정', '2025-02-01', '2025-02-28', '2025-03-01', '2025-08-31', 0, '월~금 09:00~18:00', '경기도 성남시 성남대로 34 하나플라자 6층', true, false, true, '없음', 'APPROVED', NOW(), NOW(), NOW(), false);

-- 10. 코딩 기초 입문 과정 (온라인) - KOSTA 종로, 웹개발(1번)
INSERT INTO course (id, academy_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (10, 1, 1, '코딩 기초 입문 과정', '2025-01-10', '2025-02-09', '2025-02-10', '2025-05-10', 0, '매일 19:00~21:00', '온라인', true, true, false, '없음', 'APPROVED', NOW(), NOW(), NOW(), false);

-- 11. 웹 개발 종합 과정 (온라인) - KOSTA 가산, 웹개발(1번)
INSERT INTO course (id, academy_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (11, 2, 1, '웹 개발 종합 과정', '2025-01-20', '2025-02-19', '2025-02-20', '2025-08-19', 0, '월수금 19:00~22:00', '온라인', true, true, false, '없음', 'APPROVED', NOW(), NOW(), NOW(), false);

-- 12. 데이터 분석가 양성 과정 (온라인) - KOSTA 분당, 데이터/AI(3번)
INSERT INTO course (id, academy_id, category_id, name, recruit_start, recruit_end, course_start, course_end, cost, class_day, location, is_kdt, is_nailbaeum, is_offline, requirement, is_approved, approved_at, created_at, updated_at, is_deleted)
VALUES (12, 3, 3, '데이터 분석가 양성 과정', '2025-02-01', '2025-03-01', '2025-03-02', '2025-09-01', 0, '화목 19:00~22:00', '온라인', true, false, false, '없음', 'APPROVED', NOW(), NOW(), NOW(), false);



-- 5. Board (커뮤니티 게시글)
-- NOTICE (공지사항)
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted)
VALUES (1, 'NOTICE', '소프트캠퍼스 오픈 안내', '안녕하세요! 소프트캠퍼스가 정식으로 오픈했습니다. 많은 관심 부탁드립니다.', 150, false, 1, NOW(), NOW(), false);

INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted)
VALUES (2, 'NOTICE', '2025년 상반기 과정 일정 안내', '2025년 상반기 과정 일정을 안내드립니다. 자세한 사항은 과정 페이지를 참고해주세요.', 89, false, 1, NOW(), NOW(), false);

-- QUESTION (문의사항)
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted)
VALUES (3, 'QUESTION', '국비 지원 관련 문의드립니다', '안녕하세요, 국비 지원 과정 신청 방법이 궁금합니다. 자세한 절차를 알려주시면 감사하겠습니다.', 45, false, 2, NOW(), NOW(), false);

INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted)
VALUES (4, 'QUESTION', '온라인 과정 수료증 발급 문의', '온라인 과정 수료 후 수료증은 어떻게 발급받을 수 있나요?', 32, false, 4, NOW(), NOW(), false);

-- COURSE_STORY (진로이야기)
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted)
VALUES (5, 'COURSE_STORY', '부트캠프 수료 후 취업 성공기', 'KOSTA 풀스택 과정을 수료하고 대기업 SI 회사에 취업했습니다. 포트폴리오 준비부터 면접까지 실제 경험을 공유합니다.', 234, false, 4, NOW(), NOW(), false);

INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted)
VALUES (6, 'COURSE_STORY', '비전공자의 개발자 전향기', '문과 출신 비전공자였지만 6개월 집중 학습으로 백엔드 개발자로 전향했습니다. 제 경험을 나눕니다.', 187, false, 5, NOW(), NOW(), false);

INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted)
VALUES (7, 'COURSE_STORY', '30대에 시작한 개발 공부', '30대 중반에 커리어를 바꾸기로 결심하고 개발 공부를 시작했습니다. 늦었다고 생각하는 분들께 용기를 드리고 싶습니다.', 156, false, 6, NOW(), NOW(), false);

-- CODING_STORY (코딩이야기)  
INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted)
VALUES (8, 'CODING_STORY', 'Spring Boot 프로젝트 배포 팁', 'Spring Boot 프로젝트를 AWS EC2에 배포하면서 겪었던 시행착오와 해결 방법을 공유합니다. Docker와 GitHub Actions를 활용한 CI/CD 구축 과정도 포함됩니다.', 198, false, 4, NOW(), NOW(), false);

INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted)
VALUES (9, 'CODING_STORY', 'React 상태관리 라이브러리 비교', 'Redux, Zustand, Recoil 등 다양한 React 상태관리 라이브러리를 실제 프로젝트에 적용해본 경험을 공유합니다.', 143, false, 5, NOW(), NOW(), false);

INSERT INTO board (id, category, title, text, hits, is_secret, account_id, created_at, updated_at, is_deleted)
VALUES (10, 'CODING_STORY', '알고리즘 문제 풀이 전략', '코딩 테스트를 준비하면서 정리한 알고리즘 문제 풀이 전략과 자주 나오는 패턴을 정리했습니다.', 167, false, 6, NOW(), NOW(), false);


-- 6. Comment (댓글)
-- 게시글 1번 (소프트캠퍼스 오픈 안내)에 대한 댓글
INSERT INTO comment (id, text, is_secret, board_id, account_id, comment_id, created_at, updated_at, is_deleted)
VALUES (1, '축하합니다! 기대됩니다.', false, 1, 2, NULL, NOW(), NOW(), false);

INSERT INTO comment (id, text, is_secret, board_id, account_id, comment_id, created_at, updated_at, is_deleted)
VALUES (2, '좋은 플랫폼이 되길 응원합니다!', false, 1, 4, NULL, NOW(), NOW(), false);

-- 게시글 3번 (국비 지원 관련 문의)에 대한 댓글
INSERT INTO comment (id, text, is_secret, board_id, account_id, comment_id, created_at, updated_at, is_deleted)
VALUES (3, 'HRD-Net에서 신청하시면 됩니다. 자세한 사항은 기관에 문의해보세요.', false, 3, 3, NULL, NOW(), NOW(), false);

-- 게시글 5번 (부트캠프 수료 후 취업 성공기)에 대한 댓글 (부모 댓글)
INSERT INTO comment (id, text, is_secret, board_id, account_id, comment_id, created_at, updated_at, is_deleted)
VALUES (4, '축하드립니다! 포트폴리오는 어떻게 준비하셨나요?', false, 5, 2, NULL, NOW(), NOW(), false);

INSERT INTO comment (id, text, is_secret, board_id, account_id, comment_id, created_at, updated_at, is_deleted)
VALUES (5, '저도 준비중인데 많은 도움이 되었습니다. 감사합니다!', false, 5, 5, NULL, NOW(), NOW(), false);

-- 게시글 5번의 4번 댓글에 대한 대댓글
INSERT INTO comment (id, text, is_secret, board_id, account_id, comment_id, created_at, updated_at, is_deleted)
VALUES (6, '깃허브에 프로젝트 3개 정도 올렸습니다. README 작성에 신경 많이 썼어요.', false, 5, 4, 4, NOW(), NOW(), false);

-- 게시글 8번 (Spring Boot 프로젝트 배포 팁)에 대한 댓글
INSERT INTO comment (id, text, is_secret, board_id, account_id, comment_id, created_at, updated_at, is_deleted)
VALUES (7, '정말 유익한 정보 감사합니다! Docker 부분이 특히 도움이 되었어요.', false, 8, 5, NULL, NOW(), NOW(), false);

INSERT INTO comment (id, text, is_secret, board_id, account_id, comment_id, created_at, updated_at, is_deleted)
VALUES (8, 'GitHub Actions 설정 파일도 공유해주실 수 있나요?', false, 8, 6, NULL, NOW(), NOW(), false);


-- 7. CourseReview (과정 리뷰)
-- 과정 1번 (Spring Boot 실무 프로젝트)에 대한 리뷰
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, created_at, updated_at, is_deleted)
VALUES (1, 4, 1, '실무에서 바로 적용 가능한 내용들로 구성되어 있어서 정말 유익했습니다. 강사님의 경험 공유도 많은 도움이 되었습니다.', 'APPROVED', NOW(), NOW(), false);

-- 과정 7번 (풀스택 개발자 부트캠프)에 대한 리뷰
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, created_at, updated_at, is_deleted)
VALUES (2, 5, 7, '6개월간 집중적으로 학습하여 웹 개발 전반을 배울 수 있었습니다. 포트폴리오 프로젝트도 취업에 큰 도움이 되었습니다.', 'APPROVED', NOW(), NOW(), false);

-- 과정 8번 (프론트엔드 개발자 양성)에 대한 리뷰
INSERT INTO course_review (id, account_id, course_id, comment, approval_status, created_at, updated_at, is_deleted)
VALUES (3, 6, 8, 'React를 중심으로 최신 프론트엔드 기술을 배울 수 있어서 좋았습니다. 실습 위주 수업이라 이해가 빨랐어요.', 'APPROVED', NOW(), NOW(), false);


-- 8. ReviewSection (리뷰 세부 평가)
-- 리뷰 1번에 대한 세부 평가
INSERT INTO review_section (review_id, section_type, score, comment, created_at, updated_at, is_deleted)
VALUES (1, 'CURRICULUM', 5, '실무 중심 커리큘럼이 훌륭합니다', NOW(), NOW(), false);

INSERT INTO review_section (review_id, section_type, score, comment, created_at, updated_at, is_deleted)
VALUES (1, 'COURSEWARE', 4, '교재와 자료가 잘 정리되어 있습니다', NOW(), NOW(), false);

INSERT INTO review_section (review_id, section_type, score, comment, created_at, updated_at, is_deleted)
VALUES (1, 'INSTRUCTOR', 5, '강사님의 실무 경험이 풍부합니다', NOW(), NOW(), false);

INSERT INTO review_section (review_id, section_type, score, comment, created_at, updated_at, is_deleted)
VALUES (1, 'EQUIPMENT', 4, '실습 환경이 좋습니다', NOW(), NOW(), false);

-- 리뷰 2번에 대한 세부 평가
INSERT INTO review_section (review_id, section_type, score, comment, created_at, updated_at, is_deleted)
VALUES (2, 'CURRICULUM', 5, '풀스택 전반을 배울 수 있습니다', NOW(), NOW(), false);

INSERT INTO review_section (review_id, section_type, score, comment, created_at, updated_at, is_deleted)
VALUES (2, 'COURSEWARE', 5, '프로젝트 중심 학습이 효과적입니다', NOW(), NOW(), false);

INSERT INTO review_section (review_id, section_type, score, comment, created_at, updated_at, is_deleted)
VALUES (2, 'INSTRUCTOR', 4, '현업 개발자 출신 강사님', NOW(), NOW(), false);

INSERT INTO review_section (review_id, section_type, score, comment, created_at, updated_at, is_deleted)
VALUES (2, 'EQUIPMENT', 5, '최신 장비로 실습 가능합니다', NOW(), NOW(), false);

-- 리뷰 3번에 대한 세부 평가
INSERT INTO review_section (review_id, section_type, score, comment, created_at, updated_at, is_deleted)
VALUES (3, 'CURRICULUM', 5, '최신 React 트렌드를 반영합니다', NOW(), NOW(), false);

INSERT INTO review_section (review_id, section_type, score, comment, created_at, updated_at, is_deleted)
VALUES (3, 'COURSEWARE', 4, '실습 예제가 풍부합니다', NOW(), NOW(), false);

INSERT INTO review_section (review_id, section_type, score, comment, created_at, updated_at, is_deleted)
VALUES (3, 'INSTRUCTOR', 5, '친절하고 설명이 명확합니다', NOW(), NOW(), false);

INSERT INTO review_section (review_id, section_type, score, comment, created_at, updated_at, is_deleted)
VALUES (3, 'EQUIPMENT', 4, '실습 환경 양호합니다', NOW(), NOW(), false);


-- 9. CourseFavorite (과정 즐겨찾기)
-- 사용자들의 과정 즐겨찾기
INSERT INTO course_favorite (account_id, course_id, created_at, updated_at, is_deleted)
VALUES (2, 1, NOW(), NOW(), false);

INSERT INTO course_favorite (account_id, course_id, created_at, updated_at, is_deleted)
VALUES (2, 7, NOW(), NOW(), false);

INSERT INTO course_favorite (account_id, course_id, created_at, updated_at, is_deleted)
VALUES (4, 2, NOW(), NOW(), false);

INSERT INTO course_favorite (account_id, course_id, created_at, updated_at, is_deleted)
VALUES (4, 8, NOW(), NOW(), false);

INSERT INTO course_favorite (account_id, course_id, created_at, updated_at, is_deleted)
VALUES (5, 7, NOW(), NOW(), false);

INSERT INTO course_favorite (account_id, course_id, created_at, updated_at, is_deleted)
VALUES (5, 10, NOW(), NOW(), false);

INSERT INTO course_favorite (account_id, course_id, created_at, updated_at, is_deleted)
VALUES (6, 8, NOW(), NOW(), false);

INSERT INTO course_favorite (account_id, course_id, created_at, updated_at, is_deleted)
VALUES (6, 12, NOW(), NOW(), false);


-- 10. CourseCurriculum (과정 커리큘럼)

-- 과정 1: Spring Boot 실무 프로젝트 커리큘럼
INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (1, 1, 'Spring Boot 기초와 환경 설정', 'Spring Boot 프로젝트 구조 이해, Gradle/Maven 빌드 도구, application.properties 설정, 프로파일 관리', 8, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (1, 2, 'REST API 설계 및 구현', 'RESTful API 설계 원칙, Controller-Service-Repository 패턴, DTO와 Entity 분리, 예외 처리', 12, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (1, 3, 'JPA와 데이터베이스 연동', 'JPA Entity 설계, 연관관계 매핑, JPQL과 QueryDSL, 트랜잭션 관리', 16, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (1, 4, 'Spring Security와 인증/인가', 'JWT 토큰 기반 인증, Spring Security 설정, Role 기반 권한 관리, OAuth2 소셜 로그인', 12, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (1, 5, '실전 프로젝트와 배포', 'Docker 컨테이너화, AWS EC2 배포, CI/CD 파이프라인 구축, 성능 최적화', 10, NOW(), NOW(), false);


-- 과정 2: React 심화 과정 커리큘럼
INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (2, 1, 'React 고급 Hooks', 'useCallback, useMemo 최적화, useRef DOM 제어, Custom Hooks 패턴, useReducer 상태 관리', 10, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (2, 2, '상태 관리 라이브러리', 'Redux Toolkit 활용, Zustand 경량 상태 관리, React Query 서버 상태 관리, Context API 최적화', 12, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (2, 3, 'TypeScript와 React', 'TypeScript 타입 시스템, Generic 컴포넌트, Props 타입 정의, 타입 안전성 보장', 10, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (2, 4, '성능 최적화와 테스트', 'React.memo와 최적화 기법, Code Splitting, Jest/React Testing Library, E2E 테스트', 10, NOW(), NOW(), false);


-- 과정 3: 데이터 엔지니어링 실무 커리큘럼
INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (3, 1, 'Python 데이터 처리 기초', 'Pandas DataFrame 조작, NumPy 배열 연산, 데이터 정제 및 전처리, CSV/JSON 파일 처리', 16, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (3, 2, 'SQL과 데이터베이스', 'SQL 쿼리 최적화, 인덱스 설계, 정규화/비정규화, PostgreSQL/MySQL 활용', 14, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (3, 3, 'ETL 파이프라인 구축', 'Apache Airflow 워크플로우, 데이터 추출/변환/적재, 배치 처리, 스케줄링', 18, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (3, 4, '빅데이터 처리', 'Apache Spark 기초, PySpark 분산 처리, 대용량 데이터 처리, 성능 튜닝', 20, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (3, 5, '데이터 웨어하우스', 'DW 설계 원칙, Fact/Dimension 테이블, Snowflake/BigQuery 활용', 12, NOW(), NOW(), false);


-- 과정 4: 클라우드 아키텍처 설계 커리큘럼
INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (4, 1, 'AWS 핵심 서비스', 'EC2 인스턴스 관리, VPC 네트워크 설계, S3 스토리지, RDS 데이터베이스', 12, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (4, 2, '컨테이너와 오케스트레이션', 'Docker 컨테이너 기술, ECS/EKS 쿠버네티스, 컨테이너 오케스트레이션, Auto Scaling', 14, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (4, 3, '서버리스 아키텍처', 'AWS Lambda 함수, API Gateway, DynamoDB, 이벤트 기반 아키텍처', 10, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (4, 4, '고가용성과 보안', 'Load Balancer, CloudFront CDN, IAM 권한 관리, 백업 및 재해 복구', 12, NOW(), NOW(), false);


-- 과정 5: DevOps 엔지니어 양성 커리큘럼
INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (5, 1, 'Linux 시스템 관리', 'Linux 명령어, 쉘 스크립트, 프로세스 관리, 시스템 모니터링', 12, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (5, 2, 'CI/CD 파이프라인', 'Jenkins/GitHub Actions, 자동화 빌드, 테스트 자동화, 배포 자동화', 16, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (5, 3, 'Infrastructure as Code', 'Terraform 인프라 관리, Ansible 구성 관리, CloudFormation, 버전 관리', 14, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (5, 4, '모니터링과 로깅', 'Prometheus/Grafana, ELK Stack, 로그 수집 및 분석, 알람 설정', 12, NOW(), NOW(), false);


-- 과정 6: AI/ML 실무 적용 커리큘럼
INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (6, 1, '머신러닝 기초', 'Scikit-learn 라이브러리, 지도/비지도 학습, 모델 평가 지표, 교차 검증', 16, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (6, 2, '딥러닝과 신경망', 'TensorFlow/PyTorch 프레임워크, CNN/RNN 모델, 전이 학습, 하이퍼파라미터 튜닝', 20, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (6, 3, '자연어 처리', 'NLP 전처리, Transformer 모델, BERT/GPT 활용, 텍스트 분류 및 생성', 18, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (6, 4, 'ML 모델 배포', 'Flask/FastAPI 서빙, Docker 컨테이너화, MLOps 파이프라인, 모델 모니터링', 14, NOW(), NOW(), false);


-- 과정 7: 풀스택 개발자 부트캠프 커리큘럼
INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (7, 1, 'HTML/CSS/JavaScript 기초', 'HTML5 시맨틱 태그, CSS Flexbox/Grid, JavaScript ES6+, DOM 조작', 40, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (7, 2, 'React 프론트엔드 개발', 'React 컴포넌트, Hooks, 상태 관리, React Router, API 연동', 60, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (7, 3, 'Node.js 백엔드 개발', 'Express.js 서버, REST API, MongoDB/MySQL, 인증/인가, 미들웨어', 60, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (7, 4, '팀 프로젝트', 'Git 협업, Agile 개발, 풀스택 프로젝트 구현, 발표 및 피드백', 80, NOW(), NOW(), false);


-- 과정 8: 프론트엔드 개발자 양성 커리큘럼
INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (8, 1, 'JavaScript 심화', '클로저, 프로토타입, 비동기 프로그래밍, Promise/Async-Await, 이벤트 루프', 40, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (8, 2, 'React 마스터', 'React 생명주기, Context API, Custom Hooks, 성능 최적화, SSR/SSG', 70, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (8, 3, '모던 프론트엔드 도구', 'Webpack/Vite 번들러, TypeScript, ESLint/Prettier, 테스팅', 50, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (8, 4, '포트폴리오 프로젝트', '실무형 프로젝트 기획, UI/UX 설계, 반응형 웹, 배포 및 최적화', 80, NOW(), NOW(), false);


-- 과정 9: 백엔드 개발자 취업 과정 커리큘럼
INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (9, 1, 'Java 프로그래밍', 'Java 기본 문법, OOP 개념, Collection Framework, 예외 처리, I/O', 50, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (9, 2, 'Spring Framework', 'Spring Core, DI/IoC, AOP, Spring MVC, MyBatis/JPA', 70, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (9, 3, '데이터베이스와 SQL', 'SQL 기초, ERD 설계, 정규화, 트랜잭션, 인덱스 최적화', 40, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (9, 4, '백엔드 프로젝트', 'REST API 설계, Spring Boot 프로젝트, Git 협업, 배포', 80, NOW(), NOW(), false);


-- 과정 10: 코딩 기초 입문 과정 커리큘럼
INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (10, 1, '프로그래밍 기초', '알고리즘과 순서도, 변수와 자료형, 조건문과 반복문, 함수의 개념', 20, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (10, 2, 'Python 기초', 'Python 문법, 리스트/딕셔너리, 파일 처리, 간단한 프로젝트', 25, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (10, 3, '웹 개발 입문', 'HTML/CSS 기초, JavaScript 기초, 간단한 웹페이지 만들기', 30, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (10, 4, '미니 프로젝트', '프로젝트 기획, 코딩 실습, 발표 및 피드백', 15, NOW(), NOW(), false);


-- 과정 11: 웹 개발 종합 과정 커리큘럼
INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (11, 1, 'HTML/CSS 마스터', 'HTML5 고급, CSS3 애니메이션, Sass/SCSS, 반응형 웹 디자인', 40, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (11, 2, 'JavaScript 중급', 'ES6+ 문법, DOM API, Fetch API, 비동기 처리, 모듈 시스템', 50, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (11, 3, 'Node.js와 Express', 'Node.js 기초, Express 서버, REST API, 데이터베이스 연동', 60, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (11, 4, '프로젝트 실습', '웹 애플리케이션 개발, Git 버전 관리, 배포 및 운영', 70, NOW(), NOW(), false);


-- 과정 12: 데이터 분석가 양성 과정 커리큘럼
INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (12, 1, '데이터 분석 기초', '통계학 기초, 확률 분포, 가설 검정, 데이터 시각화 원리', 30, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (12, 2, 'Python 데이터 분석', 'Pandas 활용, NumPy 연산, Matplotlib/Seaborn 시각화, Jupyter Notebook', 40, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (12, 3, 'SQL과 데이터베이스', 'SQL 쿼리 작성, Join/서브쿼리, 집계 함수, 데이터 추출 및 분석', 35, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (12, 4, '머신러닝 입문', 'Scikit-learn 기초, 회귀/분류 모델, 모델 평가, 예측 분석', 40, NOW(), NOW(), false);

INSERT INTO course_curriculum (course_id, chapter_number, chapter_name, chapter_detail, chapter_time, created_at, updated_at, is_deleted)
VALUES (12, 5, '데이터 분석 프로젝트', '실제 데이터셋 분석, 인사이트 도출, 시각화 리포트, 발표', 35, NOW(), NOW(), false);


-- 11. CourseQna (과정 Q&A)

-- 과정 1 (Spring Boot) Q&A
INSERT INTO course_qna (course_id, writer_id, answered_by_id, title, question_text, answer_text, is_answered, created_at, updated_at, is_deleted)
VALUES (1, 2, 3, 'JPA N+1 문제 해결 방법 문의', '강의에서 배운 JPA를 사용하다가 N+1 문제가 발생했는데, 어떻게 해결하면 좋을까요?', 'Fetch Join이나 @EntityGraph를 사용하면 N+1 문제를 해결할 수 있습니다. 또한 Batch Size를 설정하는 방법도 있으니 상황에 맞게 선택하시면 됩니다.', true, NOW(), NOW(), false);

INSERT INTO course_qna (course_id, writer_id, answered_by_id, title, question_text, answer_text, is_answered, created_at, updated_at, is_deleted)
VALUES (1, 4, NULL, 'Docker 배포 시 환경변수 관리', 'Docker로 배포할 때 환경변수를 어떻게 관리하는 것이 좋을까요? .env 파일을 사용해도 되나요?', NULL, false, NOW(), NOW(), false);

-- 과정 2 (React) Q&A
INSERT INTO course_qna (course_id, writer_id, answered_by_id, title, question_text, answer_text, is_answered, created_at, updated_at, is_deleted)
VALUES (2, 5, 3, 'useEffect 의존성 배열 관련 질문', 'useEffect의 의존성 배열에 함수를 넣어야 할 때가 있는데, 매번 리렌더링되는 것 같아요. 어떻게 최적화하나요?', 'useCallback을 사용하여 함수를 메모이제이션하면 됩니다. 또는 useRef를 활용하여 최신 값을 참조하는 방법도 있습니다.', true, NOW(), NOW(), false);

INSERT INTO course_qna (course_id, writer_id, answered_by_id, title, question_text, answer_text, is_answered, created_at, updated_at, is_deleted)
VALUES (2, 6, 1, 'React Query와 Redux 동시 사용', 'React Query를 사용하면 Redux가 필요 없나요? 둘을 같이 써도 되나요?', 'React Query는 서버 상태 관리에 특화되어 있고, Redux는 클라이언트 상태 관리에 적합합니다. 필요에 따라 함께 사용해도 무방합니다.', true, NOW(), NOW(), false);

-- 과정 7 (풀스택 부트캠프) Q&A
INSERT INTO course_qna (course_id, writer_id, answered_by_id, title, question_text, answer_text, is_answered, created_at, updated_at, is_deleted)
VALUES (7, 4, 3, '프로젝트 주제 선정 조언', '팀 프로젝트 주제를 선정하려고 하는데, 포트폴리오로 활용하기 좋은 주제가 무엇일까요?', '실제 문제를 해결하는 서비스를 만들어보세요. 예를 들어 커뮤니티, 예약 시스템, 관리 대시보드 등이 좋습니다. CRUD 기능과 인증이 포함되면 더욱 좋습니다.', true, NOW(), NOW(), false);

INSERT INTO course_qna (course_id, writer_id, answered_by_id, title, question_text, answer_text, is_answered, created_at, updated_at, is_deleted)
VALUES (7, 5, NULL, 'Git 협업 시 충돌 해결', 'Git으로 팀 작업을 하다가 자주 충돌이 발생하는데, 좋은 해결 방법이 있을까요?', NULL, false, NOW(), NOW(), false);

-- 과정 8 (프론트엔드) Q&A
INSERT INTO course_qna (course_id, writer_id, answered_by_id, title, question_text, answer_text, is_answered, created_at, updated_at, is_deleted)
VALUES (8, 6, 1, 'TypeScript 타입 에러 해결', 'any 타입을 최대한 사용하지 않으려고 하는데, 복잡한 객체의 타입을 어떻게 정의해야 할까요?', 'interface나 type을 사용하여 명시적으로 정의하세요. 중첩 객체는 단계별로 타입을 나누어 정의하면 관리가 쉽습니다.', true, NOW(), NOW(), false);

-- 과정 9 (백엔드) Q&A
INSERT INTO course_qna (course_id, writer_id, answered_by_id, title, question_text, answer_text, is_answered, created_at, updated_at, is_deleted)
VALUES (9, 2, 3, 'Spring Security 설정 질문', 'Spring Security를 처음 사용하는데 너무 어렵습니다. 기본적인 인증 설정은 어떻게 하나요?', '최신 Spring Security는 SecurityFilterChain 빈을 등록하는 방식입니다. 강의 자료의 예제 코드를 참고하시고, 단계별로 따라해보세요.', true, NOW(), NOW(), false);

INSERT INTO course_qna (course_id, writer_id, answered_by_id, title, question_text, answer_text, is_answered, created_at, updated_at, is_deleted)
VALUES (9, 4, NULL, 'REST API 설계 모범 사례', 'RESTful API를 설계할 때 URL 네이밍 규칙이나 HTTP 메서드 선택 기준이 궁금합니다.', NULL, false, NOW(), NOW(), false);

-- 과정 10 (코딩 기초) Q&A
INSERT INTO course_qna (course_id, writer_id, answered_by_id, title, question_text, answer_text, is_answered, created_at, updated_at, is_deleted)
VALUES (10, 5, 1, '프로그래밍 공부 방법', '비전공자인데 프로그래밍 공부를 어떻게 시작하는 것이 좋을까요?', '기초 문법을 배운 후 작은 프로젝트를 직접 만들어보는 것이 가장 효과적입니다. 에러를 겪고 해결하는 과정에서 많이 배우게 됩니다.', true, NOW(), NOW(), false);

-- 과정 12 (데이터 분석) Q&A
INSERT INTO course_qna (course_id, writer_id, answered_by_id, title, question_text, answer_text, is_answered, created_at, updated_at, is_deleted)
VALUES (12, 6, 3, 'Pandas 데이터 전처리 팁', '데이터 전처리를 할 때 결측치 처리 기준이 궁금합니다. 삭제와 대체 중 어떤 것을 선택해야 하나요?', '데이터의 특성과 결측치 비율에 따라 다릅니다. 결측치가 적으면 삭제, 많으면 평균/중앙값/최빈값으로 대체하거나 예측 모델을 사용합니다.', true, NOW(), NOW(), false);


-- 12. ReviewLike (리뷰 좋아요/싫어요)

-- 리뷰 1번 (Spring Boot 과정)에 대한 좋아요/싫어요
INSERT INTO review_like (review_id, account_id, type, created_at, updated_at, is_deleted)
VALUES (1, 2, 'LIKE', NOW(), NOW(), false);

INSERT INTO review_like (review_id, account_id, type, created_at, updated_at, is_deleted)
VALUES (1, 5, 'LIKE', NOW(), NOW(), false);

INSERT INTO review_like (review_id, account_id, type, created_at, updated_at, is_deleted)
VALUES (1, 6, 'LIKE', NOW(), NOW(), false);

-- 리뷰 2번 (풀스택 부트캠프)에 대한 좋아요/싫어요
INSERT INTO review_like (review_id, account_id, type, created_at, updated_at, is_deleted)
VALUES (2, 2, 'LIKE', NOW(), NOW(), false);

INSERT INTO review_like (review_id, account_id, type, created_at, updated_at, is_deleted)
VALUES (2, 4, 'LIKE', NOW(), NOW(), false);

INSERT INTO review_like (review_id, account_id, type, created_at, updated_at, is_deleted)
VALUES (2, 6, 'LIKE', NOW(), NOW(), false);

-- 리뷰 3번 (프론트엔드 과정)에 대한 좋아요/싫어요
INSERT INTO review_like (review_id, account_id, type, created_at, updated_at, is_deleted)
VALUES (3, 2, 'LIKE', NOW(), NOW(), false);

INSERT INTO review_like (review_id, account_id, type, created_at, updated_at, is_deleted)
VALUES (3, 4, 'LIKE', NOW(), NOW(), false);

INSERT INTO review_like (review_id, account_id, type, created_at, updated_at, is_deleted)
VALUES (3, 5, 'LIKE', NOW(), NOW(), false);

INSERT INTO review_like (review_id, account_id, type, created_at, updated_at, is_deleted)
VALUES (3, 1, 'DISLIKE', NOW(), NOW(), false);






