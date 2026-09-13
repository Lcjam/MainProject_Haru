-- V1 — Board·Location 결손 테이블 복원 (F3)
--
-- 배경: 매퍼가 참조하는 테이블 7종과 Users 컬럼 1개가 haru_db 에 없어서 해당 엔드포인트가
-- MySQL 1146/1054 → HTTP 500 이었다. 리포에 이 테이블들의 DDL 이 없다
-- (MySQL/board_table/ 의 .txt 는 Posts(post_id)·Boards(board_id) 구세대라 현재 매퍼와 불일치).
--
-- 근거: 각 컬럼은 매퍼 XML 의 INSERT 컬럼 목록·WHERE/ORDER BY·resultMap 과
-- model 클래스의 필드 타입에서 역산했다. 컬럼별 출처를 아래 주석에 병기한다.
-- 타입·인덱스·FK 는 기존 테이블 관례를 따랐다(이미지 경로 varchar(500) = ProductImages,
-- 위도/경도 decimal(10,8)/(11,8) = UserLocation, FK ON DELETE CASCADE = boards/ProductImages).
--
-- 적용: mysql -u root < sql/migrations/V1__board_location_tables.sql
-- (schema.sql 은 재덤프하지 않는다 — sql/README.md 규칙)

USE `haru_db`;

-- ---------------------------------------------------------------------------
-- 1. Users.profile_image_path — 컬럼 결손
--    UserMapper(updateUserProfile / anonymizeUserData / updateProfileImagePath) 와
--    Board 계열 4개 매퍼의 JOIN(u.profile_image_path)이 참조. 없으면 1054.
--    구세대 MySQL/User_table/User/Users.txt 에도 없다 — 코드에만 추가되고 미반영된 컬럼.
-- ---------------------------------------------------------------------------
ALTER TABLE `Users`
    ADD COLUMN `profile_image_path` VARCHAR(255) DEFAULT NULL AFTER `bio`;

-- ---------------------------------------------------------------------------
-- 2. board_posts — PostMapper (model/board/Post.java)
-- ---------------------------------------------------------------------------
CREATE TABLE `board_posts` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,          -- Post.id(Long), useGeneratedKeys
  `board_id`      BIGINT       NOT NULL,                          -- createPost / getPostsByBoardId
  `author_email`  VARCHAR(255) NOT NULL,                          -- createPost, JOIN users.email
  `title`         VARCHAR(255) NOT NULL,                          -- searchPosts LIKE 대상
  `content`       TEXT         NOT NULL,                          -- searchPosts LIKE 대상
  `created_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP,         -- ORDER BY created_at DESC
  `updated_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at`    DATETIME     DEFAULT NULL,                      -- deletePost (소프트 삭제)
  `is_deleted`    TINYINT(1)   NOT NULL DEFAULT 0,                -- 전 쿼리의 필터 조건
  `view_count`    INT          NOT NULL DEFAULT 0,                -- increaseViewCount, 정렬 키
  `like_count`    INT          NOT NULL DEFAULT 0,                -- increment/decrementLikeCount
  `comment_count` INT          NOT NULL DEFAULT 0,                -- CommentMapper.updatePostCommentCount
  PRIMARY KEY (`id`),
  KEY `idx_board_posts_board` (`board_id`, `is_deleted`, `created_at`),
  KEY `idx_board_posts_author` (`author_email`),
  CONSTRAINT `board_posts_ibfk_1` FOREIGN KEY (`board_id`) REFERENCES `boards` (`id`) ON DELETE CASCADE,
  CONSTRAINT `board_posts_ibfk_2` FOREIGN KEY (`author_email`) REFERENCES `Users` (`email`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 3. board_post_images — PostMapper.addPostImage / getPostImagesByPostId
--    (model/board/PostImage.java)
-- ---------------------------------------------------------------------------
CREATE TABLE `board_post_images` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `post_id`    BIGINT       NOT NULL,
  `image_url`  VARCHAR(500) NOT NULL,                             -- ProductImages.image_path 와 동일 폭
  `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP,            -- ORDER BY created_at ASC
  PRIMARY KEY (`id`),
  KEY `idx_board_post_images_post` (`post_id`),
  CONSTRAINT `board_post_images_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `board_posts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 4. board_comments — CommentMapper (model/board/Comment.java)
--    parent_id 자기참조로 1단계 대댓글(depth 0/1)을 표현한다.
-- ---------------------------------------------------------------------------
CREATE TABLE `board_comments` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `post_id`      BIGINT       NOT NULL,
  `parent_id`    BIGINT       DEFAULT NULL,                       -- NULL = 최상위 댓글
  `author_email` VARCHAR(255) NOT NULL,
  `content`      TEXT         NOT NULL,
  `created_at`   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at`   DATETIME     DEFAULT NULL,                       -- deleteComment (소프트 삭제)
  `is_deleted`   TINYINT(1)   NOT NULL DEFAULT 0,
  `depth`        INT          NOT NULL DEFAULT 0,                 -- createComment: parentId 유무로 0/1
  `reply_count`  INT          NOT NULL DEFAULT 0,                 -- updateReplyCount 가 갱신
  PRIMARY KEY (`id`),
  KEY `idx_board_comments_post` (`post_id`, `is_deleted`, `created_at`),
  KEY `idx_board_comments_parent` (`parent_id`, `is_deleted`),
  KEY `idx_board_comments_author` (`author_email`),
  CONSTRAINT `board_comments_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `board_posts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `board_comments_ibfk_2` FOREIGN KEY (`parent_id`) REFERENCES `board_comments` (`id`) ON DELETE CASCADE,
  CONSTRAINT `board_comments_ibfk_3` FOREIGN KEY (`author_email`) REFERENCES `Users` (`email`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 5. board_members — BoardMapper(멤버 수 서브쿼리) + BoardMemberMapper
--    (model/board/BoardMember.java)
--
--    ⚠️ 코드 불일치를 그대로 수용: addMember 는 created_at 에 INSERT 하는데
--       resultMap 은 joinedAt <- joined_at 을 읽는다. 두 컬럼을 모두 두되
--       joined_at 에 DEFAULT CURRENT_TIMESTAMP 를 줘서 조회 시 NULL 이 되지 않게 한다.
--       (컬럼 일원화는 스키마 복원 범위 밖 — 코드 정리 시 함께 처리)
-- ---------------------------------------------------------------------------
CREATE TABLE `board_members` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,              -- selectKey LAST_INSERT_ID (Long)
  `board_id`   BIGINT       NOT NULL,
  `user_email` VARCHAR(255) NOT NULL,
  `role`       VARCHAR(20)  NOT NULL DEFAULT 'MEMBER',            -- HOST / ADMIN / MEMBER (정렬에 ADMIN 사용)
  `status`     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',            -- ACTIVE / PENDING / BANNED
  `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP,            -- addMember 가 기록
  `joined_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP,            -- resultMap 이 조회
  `invited_by` VARCHAR(255) DEFAULT NULL,                         -- resultMap 전용(현재 쓰기 없음)
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_board_members_board_user` (`board_id`, `user_email`),
  KEY `idx_board_members_user` (`user_email`, `status`),
  CONSTRAINT `board_members_ibfk_1` FOREIGN KEY (`board_id`) REFERENCES `boards` (`id`) ON DELETE CASCADE,
  CONSTRAINT `board_members_ibfk_2` FOREIGN KEY (`user_email`) REFERENCES `Users` (`email`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 6. board_post_reactions — PostReactionMapper (model/board/PostReaction.java)
--    updateReaction/deleteReaction/hasUserReacted 가 (post_id, user_email) 을 키처럼
--    다루므로 유니크 제약을 건다 — 사용자당 게시글 1반응.
-- ---------------------------------------------------------------------------
CREATE TABLE `board_post_reactions` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `post_id`       BIGINT       NOT NULL,
  `user_email`    VARCHAR(255) NOT NULL,
  `reaction_type` VARCHAR(20)  NOT NULL,                          -- LIKE / LOVE / HAHA / WOW / SAD / ANGRY
  `created_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_board_post_reactions_post_user` (`post_id`, `user_email`),
  KEY `idx_board_post_reactions_user` (`user_email`),
  CONSTRAINT `board_post_reactions_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `board_posts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `board_post_reactions_ibfk_2` FOREIGN KEY (`user_email`) REFERENCES `Users` (`email`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 7. locations — LocationMapper (model/Location.java). 채팅방 내 실시간 위치 공유.
--    UserLocation(프로필 위치)과는 별개 테이블이다.
--    deleteOldLocations 가 24시간 초과분을 지우므로 timestamp 인덱스가 필요하다.
-- ---------------------------------------------------------------------------
CREATE TABLE `locations` (
  `location_id`  BIGINT        NOT NULL AUTO_INCREMENT,           -- resultMap 의 <id> 컬럼
  `chatroom_id`  INT           NOT NULL,                          -- Location.chatroomId(Integer) = chatrooms.chatroom_id(int)
  `email`        VARCHAR(255)  NOT NULL,
  `latitude`     DECIMAL(10,8) NOT NULL,                          -- UserLocation 과 동일 정밀도
  `longitude`    DECIMAL(11,8) NOT NULL,
  `timestamp`    DATETIME      NOT NULL,                          -- 앱이 넘기는 측정 시각(서버 시각과 별개)
  `created_at`   DATETIME      DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`location_id`),
  KEY `idx_locations_room_time` (`chatroom_id`, `timestamp`),
  KEY `idx_locations_email` (`email`),
  KEY `idx_locations_timestamp` (`timestamp`),
  CONSTRAINT `locations_ibfk_1` FOREIGN KEY (`chatroom_id`) REFERENCES `chatrooms` (`chatroom_id`) ON DELETE CASCADE,
  CONSTRAINT `locations_ibfk_2` FOREIGN KEY (`email`) REFERENCES `Users` (`email`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 8. User_Account_info — UserMapper.updateUserAccountInfoStatus (WithdrawalService:95)
--    이 DDL 은 역산이 아니라 리포의 구세대 스크립트
--    MySQL/User_table/User/UserAccount_info.txt 를 그대로 옮긴 것이다.
--
--    ⚠️ 현재 코드에 INSERT 가 없어 테이블은 비어 있고 위 UPDATE 는 0행 갱신이 된다.
--       Users.account_status 와 중복되는 레거시 구조 — 제거 후보다.
--       다만 테이블이 없으면 탈퇴 경로가 1146 으로 터지므로 지금은 만들어 둔다.
-- ---------------------------------------------------------------------------
CREATE TABLE `User_Account_info` (
  `email`          VARCHAR(255) NOT NULL,
  `account_status` ENUM('Active','Deactivated','Dormant','Withdrawal') NOT NULL,
  `authority`      ENUM('1','2') NOT NULL,                        -- '1': 일반, '2': 관리자
  `authority_name` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`email`),
  CONSTRAINT `fk_user_account_email` FOREIGN KEY (`email`) REFERENCES `Users` (`email`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
