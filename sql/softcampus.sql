CREATE TABLE `courseCategories` (
	`카테고리 고유 ID`	BIGINT	NOT NULL,
	`카테고리명`	VARCHAR(10)	NULL,
	`Field2`	VARCHAR(255)	NULL
);

CREATE TABLE `course_favorite` (
	`id`	int	NOT NULL	COMMENT 'AUTO INCREMENT',
	`account_id`	int	NOT NULL,
	`course_id`	VARCHAR(255)	NOT NULL,
	`created_at`	TIMESTAMP	NULL,
	`updated_at`	TIMESTAMP	NULL,
	`is_deleted`	ENUM	NULL,
	`deleted_at`	TIMESTAMP	NULL
);

CREATE TABLE `courses` (
	`과정 고유 ID`	BIGINT	NOT NULL	COMMENT 'AUTO INCREMENT',
	`카테고리 대분류`	ENUM	NOT NULL	COMMENT ''재직자', '채용예정자'',
	`세부 카테고리`	VARCHAR(50)	NOT NULL,
	`과정 제목`	VARCHAR(100)	NOT NULL,
	`과정 설명`	TEXT	NULL,
	`승인 상태`	ENUM	NOT NULL	DEFAULT '승인대기'	COMMENT ''승인대기', '승인완료', '삭제대기', '삭제완료'',
	`등록일`	TIMESTAMP	NOT NULL	DEFAULT NOW()	COMMENT 'DEFAULT now()',
	`수정일`	TIMESTAMP	NULL
);

CREATE TABLE `academy_question` (
	`id`	int	NOT NULL	COMMENT 'AUTO INCREMENT',
	`question_number`	VARCHAR(255)	NULL,
	`title`	VARCHAR(255)	NULL,
	`text`	TEXT	NULL,
	`created_at`	TIMESTAMP	NULL,
	`updated_at`	TIMESTAMP	NULL,
	`is_deleted`	ENUM	NULL,
	`deleted_at`	TIMESTAMP	NULL,
	`academy_id`	INT	NOT NULL	COMMENT 'AUTO INCREMENT'
);

CREATE TABLE `postCategories` (

);

CREATE TABLE `board` (
	`id`	INT	NOT NULL	COMMENT 'AUTO_INCREMENT',
	`account_id`	int	NOT NULL,
	`category`	ENUM("공지사항","문의사항","진로이야기","코딩이야기"),	NOT NULL,
	`title`	VARCHAR(255)	NOT NULL,
	`text`	TEXT	NOT NULL,
	`hit`	INT	NOT NULL,
	`recommend`	INT	NOT NULL,
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NULL,
	`deleted_at`	DATETIME	NULL,
	`is_deleted`	TINYINT(1)	NOT NULL
);

CREATE TABLE `posts` (

);

CREATE TABLE `courseReviews` (
	`리뷰 고유 ID`	BIGINT	NOT NULL,
	`Field`	VARCHAR(255)	NULL,
	`Field2`	VARCHAR(255)	NULL
);

CREATE TABLE `course` (
	`id`	int	NOT NULL	COMMENT 'AUTO INCREMENT',
	`academy_id`	VARCHAR(255)	NOT NULL,
	`course_category_id`	VARCHAR(255)	NOT NULL,
	`name`	VARCHAR(255)	NULL,
	`recrut_start`	TIMESTAMP	NULL,
	`recrut_end`	TIMESTAMP	NULL,
	`course_start`	TIMESTAMP	NULL,
	`course_end`	VARCHAR(255)	NULL,
	`cost`	VARCHAR(255)	NULL,
	`class_day`	VARCHAR(255)	NULL,
	`location`	VARCHAR(255)	NULL,
	`is_nailbaeum`	ENUM	NULL,
	`is_kdt`	ENUM	NULL,
	`requirement`	VARCHAR(255)	NULL,
	`is_approved`	ENUM	NULL,
	`created_at`	TIMESTAMP	NULL,
	`updated_at`	TIMESTAMP	NULL,
	`approved_at`	TIMESTAMP	NULL,
	`is_deleted`	ENUM	NULL,
	`deleted_at`	TIMESTAMP	NULL,
	`is_offline`	ENUM	NULL
);

CREATE TABLE `board_attach` (
	`id`	INT	NOT NULL	COMMENT 'AUTO_INCREMENT',
	`Field`	VARCHAR(255)	NULL
);

CREATE TABLE `course_image` (
	`id`	int	NOT NULL
);

CREATE TABLE `course_review` (
	`id`	INT	NOT NULL	COMMENT 'AUTO INCREMENT',
	`course_id`	INT	NOT NULL,
	`title`	VARCHAR(255)	NULL,
	`section1_point`	INT	NULL,
	`section1_text`	TEXT	NULL,
	`section2_point`	INT	NULL,
	`section2_text`	TEXT	NULL,
	`section3_point`	INT	NULL,
	`section3_text`	TEXT	NULL,
	`section4_point`	INT	NULL,
	`section4_text`	TEXT	NULL,
	`section5_point`	INT	NULL,
	`section5_text`	TEXT	NULL,
	`section6_point`	INT	NULL,
	`section6_text`	TEXT	NULL,
	`created_at`	TIMESTAMP	NULL,
	`updated_at`	TIMESTAMP	NULL,
	`course_review_approved`	ENUM	NULL,
	`approved_at`	TIMESTAMP	NULL,
	`is_deleted`	ENUM	NULL,
	`deleted_at`	TIMESTAMP	NULL
);

CREATE TABLE `board_category` (
	`id`	int	NOT NULL	COMMENT 'AUTO INCREMENT',
	`name`	VARCHAR(255)	NULL,
	`created_at`	VARCHAR(255)	NULL,
	`updated_at`	VARCHAR(255)	NULL,
	`is_deleted`	VARCHAR(255)	NULL,
	`deleted_at`	VARCHAR(255)	NULL,
	`last_number`	VARCHAR(255)	NULL
);

CREATE TABLE `course_answer` (
	`id`	int	NOT NULL	COMMENT 'AUTO INCREMENT',
	`course_question_id`	VARCHAR(255)	NOT NULL,
	`text`	TEXT	NULL,
	`created_at`	TIMESTAMP	NULL,
	`updated_at`	TIMESTAMP	NULL,
	`is_deleted`	ENUM	NULL,
	`deleted_at`	TIMESTAMP	NULL,
	`is_approved`	ENUM	NULL,
	`approved_at`	TIMESTAMP	NULL
);

CREATE TABLE `courseLikes` (

);

CREATE TABLE `course_category` (
	`id`	int	NOT NULL	COMMENT 'AUTO INCREMENT',
	`category_name`	VARCHAR(255)	NULL,
	`category_type`	ENUM	NOT NULL	COMMENT '재직자, 취업예정자',
	`created_at`	TIMESTAMP	NULL,
	`updated_at`	TIMESTAMP	NULL,
	`is_deleted`	ENUM	NULL,
	`deleted_at`	TIMESTAMP	NULL
);

CREATE TABLE `course_curriculum` (
	`id`	VARCHAR(255)	NOT NULL,
	`id2`	int	NOT NULL	COMMENT 'AUTO INCREMENT',
	`chapter_number`	int	NOT NULL,
	`chapter_name`	VARCHAR(255)	NULL,
	`chapter_detail`	text	NULL,
	`chapter_time`	int	NOT NULL,
	`course_tag_id`	int	NOT NULL
);

CREATE TABLE `account` (
	`id`	int	NOT NULL	COMMENT 'AUTO INCREMENT',
	`email`	VARCHAR(255)	NULL	COMMENT 'UNIQUE',
	`account_type`	ENUM	NOT NULL	COMMENT ''사용자', '기관', '관리자'',
	`nickname`	VARCHAR(255)	NULL,
	`password`	VARCHAR(255)	NOT NULL,
	`address`	VARCHAR(255)	NULL,
	`affiliation`	VARCHAR(255)	NULL,
	`position`	VARCHAR(255)	NULL,
	`created_at`	TIMESTAMP	NOT NULL,
	`is_deleted`	ENUM	NOT NULL,
	`updated_at`	TIMESTAMP	NOT NULL,
	`deleted_at`	TIMESTAMP	NOT NULL,
	`account_approved`	ENUM	NOT NULL,
	`academy_id`	INT	NULL
);

CREATE TABLE `file` (
	`id`	int	NOT NULL	COMMENT 'AUTO INCREMENT',
	`category_type`	VARCHAR(255)	NULL,
	`category_id`	VARCHAR(255)	NULL,
	`filename`	VARCHAR(255)	NULL,
	`origin_name`	VARCHAR(255)	NULL,
	`created_at`	VARCHAR(255)	NULL,
	`is_deleted`	VARCHAR(255)	NULL,
	`deleted_at`	VARCHAR(255)	NULL
);

CREATE TABLE `courseQnA` (

);

CREATE TABLE `academy_answer` (
	`id`	int	NOT NULL	COMMENT 'AUTO INCREMENT',
	`text`	TEXT	NULL,
	`created_at`	TIMESTAMP	NULL,
	`updated_at`	TIMESTAMP	NULL,
	`is_deleted`	ENUM	NULL,
	`deleted_at`	TIMESTAMP	NULL,
	`is_approved`	ENUM	NULL,
	`approved_at`	TIMESTAMP	NULL,
	`academy_question_id`	int	NOT NULL	COMMENT 'AUTO INCREMENT'
);

CREATE TABLE `course_question` (
	`id`	int	NOT NULL	COMMENT 'AUTO INCREMENT',
	`question_number`	VARCHAR(255)	NULL,
	`title`	VARCHAR(255)	NULL,
	`text`	TEXT	NULL,
	`created_at`	TIMESTAMP	NULL,
	`updated_at`	TIMESTAMP	NULL,
	`is_deleted`	ENUM	NULL,
	`deleted_at`	TIMESTAMP	NULL,
	`course_id`	int	NOT NULL	COMMENT 'AUTO INCREMENT'
);

CREATE TABLE `course_tag` (
	`id`	VARCHAR(255)	NOT NULL,
	`name`	VARCHAR(255)	NULL	COMMENT 'UNIQUE'
);

CREATE TABLE `institutions` (
	`훈련기관 ID`	BIGINT	NOT NULL	COMMENT 'AUTO INCREMENT',
	`기관명`	VARCHAR(50)	NULL,
	`기관 주소`	VARCHAR(100)	NULL,
	`설명`	VARCHAR(250)	NULL
);

CREATE TABLE `comment` (
	`id`	INT	NOT NULL	COMMENT 'AUTO INCREMENT',
	`board_id`	INT	NOT NULL,
	`comment_id`	INT	NULL,
	`comment`	TEXT	NULL,
	`recommend`	INT	NULL,
	`created_at`	VARCHAR(255)	NULL,
	`updated_at`	VARCHAR(255)	NULL,
	`deleted_at`	VARCHAR(255)	NULL,
	`is_deleted`	VARCHAR(255)	NULL
);

CREATE TABLE `users` (
	`사용자 고유 Id`	BIGINT	NOT NULL	COMMENT 'AUTO INCREMENT',
	`사용자 로그인 id (이메일)`	VARCHAR(255)	NOT NULL	COMMENT 'UNIQUE',
	`비밀번호`	VARCHAR(255)	NOT NULL,
	`사용자 타입`	ENUM	NOT NULL	COMMENT ''사용자', '기관', '관리자'',
	`이름`	VARCHAR(50)	NULL,
	`Field`	VARCHAR(255)	NULL
);

CREATE TABLE `academy` (
	`id`	INT	NOT NULL	COMMENT 'AUTO INCREMENT',
	`name`	VARCHAR(255)	NULL,
	`address`	VARCHAR(255)	NULL,
	`business_number`	VARCHAR(255)	NULL,
	`phone`	VARCHAR(255)	NULL,
	`email`	VARCHAR(255)	NULL,
	`created_at`	TIMESTAMP	NULL,
	`updated_at`	TIMESTAMP	NULL,
	`is_deleted`	ENUM	NULL,
	`deleted_at`	TIMESTAMP	NULL,
	`is_approved`	ENUM	NULL,
	`approved_at`	ENUM	NULL
);

ALTER TABLE `courseCategories` ADD CONSTRAINT `PK_COURSECATEGORIES` PRIMARY KEY (
	`카테고리 고유 ID`
);

ALTER TABLE `course_favorite` ADD CONSTRAINT `PK_COURSE_FAVORITE` PRIMARY KEY (
	`id`
);

ALTER TABLE `courses` ADD CONSTRAINT `PK_COURSES` PRIMARY KEY (
	`과정 고유 ID`
);

ALTER TABLE `academy_question` ADD CONSTRAINT `PK_ACADEMY_QUESTION` PRIMARY KEY (
	`id`
);

ALTER TABLE `board` ADD CONSTRAINT `PK_BOARD` PRIMARY KEY (
	`id`
);

ALTER TABLE `courseReviews` ADD CONSTRAINT `PK_COURSEREVIEWS` PRIMARY KEY (
	`리뷰 고유 ID`
);

ALTER TABLE `course` ADD CONSTRAINT `PK_COURSE` PRIMARY KEY (
	`id`
);

ALTER TABLE `board_attach` ADD CONSTRAINT `PK_BOARD_ATTACH` PRIMARY KEY (
	`id`
);

ALTER TABLE `course_image` ADD CONSTRAINT `PK_COURSE_IMAGE` PRIMARY KEY (
	`id`
);

ALTER TABLE `course_review` ADD CONSTRAINT `PK_COURSE_REVIEW` PRIMARY KEY (
	`id`
);

ALTER TABLE `board_category` ADD CONSTRAINT `PK_BOARD_CATEGORY` PRIMARY KEY (
	`id`
);

ALTER TABLE `course_answer` ADD CONSTRAINT `PK_COURSE_ANSWER` PRIMARY KEY (
	`id`
);

ALTER TABLE `course_category` ADD CONSTRAINT `PK_COURSE_CATEGORY` PRIMARY KEY (
	`id`
);

ALTER TABLE `course_curriculum` ADD CONSTRAINT `PK_COURSE_CURRICULUM` PRIMARY KEY (
	`id`
);

ALTER TABLE `account` ADD CONSTRAINT `PK_ACCOUNT` PRIMARY KEY (
	`id`
);

ALTER TABLE `file` ADD CONSTRAINT `PK_FILE` PRIMARY KEY (
	`id`
);

ALTER TABLE `academy_answer` ADD CONSTRAINT `PK_ACADEMY_ANSWER` PRIMARY KEY (
	`id`
);

ALTER TABLE `course_question` ADD CONSTRAINT `PK_COURSE_QUESTION` PRIMARY KEY (
	`id`
);

ALTER TABLE `course_tag` ADD CONSTRAINT `PK_COURSE_TAG` PRIMARY KEY (
	`id`
);

ALTER TABLE `institutions` ADD CONSTRAINT `PK_INSTITUTIONS` PRIMARY KEY (
	`훈련기관 ID`
);

ALTER TABLE `comment` ADD CONSTRAINT `PK_COMMENT` PRIMARY KEY (
	`id`
);

ALTER TABLE `users` ADD CONSTRAINT `PK_USERS` PRIMARY KEY (
	`사용자 고유 Id`
);

ALTER TABLE `academy` ADD CONSTRAINT `PK_ACADEMY` PRIMARY KEY (
	`id`
);

