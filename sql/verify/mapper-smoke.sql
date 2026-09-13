-- 과거 수동 매퍼 스모크 fixture. 파라미터를 리터럴로 치환한 SQL이며
-- 현재 MyBatis의 동적 SQL/매핑/트랜잭션을 자동 검증하는 테스트는 아니다.
-- R02의 검증 명령·범위는 sql/README.md, 실제 MyBatis 통합은 R03에서 다룬다.
--
-- ⚠️ **데이터를 INSERT 하므로 스크래치 DB 에서만 실행할 것.** 운영/개발 haru_db 금지.
--
-- 수동 재사용 전 전용 haru_db_verify를 R02 실행기로 초기화하고 이 DB에만
-- 합성 사용자/참조 데이터를 준비해야 한다. schema.sql 직접 재실행은 금지한다.
-- seed-local.sql은 USE haru_db를 포함하므로 이 DB에 그대로 실행하면 안 된다.
--
-- 마지막 메시지는 이 파일에 복사된 SQL의 완료 표시일 뿐 전체 매퍼 통과가 아니다.

USE `haru_db_verify`;

-- 준비: 게시판 1개 (BoardMapper.createBoard 그대로)
INSERT INTO boards (name, description, image_path, host_email, created_at, updated_at, status)
VALUES ('테스트 게시판', '설명', NULL, 'test1@haru.com', NOW(), NOW(), 'ACTIVE');
SET @board_id = LAST_INSERT_ID();

-- BoardMemberMapper.addMember
INSERT INTO board_members (board_id, user_email, role, status, created_at)
VALUES (@board_id, 'test1@haru.com', 'HOST', 'ACTIVE', NOW());
INSERT INTO board_members (board_id, user_email, role, status, created_at)
VALUES (@board_id, 'test2@haru.com', 'MEMBER', 'ACTIVE', NOW());

-- PostMapper.createPost
INSERT INTO board_posts (board_id, author_email, title, content, created_at, updated_at, is_deleted, view_count, like_count, comment_count)
VALUES (@board_id, 'test1@haru.com', '첫 글', '본문입니다', NOW(), NOW(), false, 0, 0, 0);
SET @post_id = LAST_INSERT_ID();

-- PostMapper.addPostImage
INSERT INTO board_post_images (post_id, image_url, created_at)
VALUES (@post_id, '/uploads/post/1.png', NOW());

-- CommentMapper.createComment (최상위 + 대댓글)
INSERT INTO board_comments (post_id, parent_id, author_email, content, created_at, updated_at, is_deleted, depth)
VALUES (@post_id, NULL, 'test2@haru.com', '댓글', NOW(), NOW(), false, 0);
SET @comment_id = LAST_INSERT_ID();
INSERT INTO board_comments (post_id, parent_id, author_email, content, created_at, updated_at, is_deleted, depth)
VALUES (@post_id, @comment_id, 'test1@haru.com', '대댓글', NOW(), NOW(), false, 1);

-- PostReactionMapper.addReaction / updateReactionType
INSERT INTO board_post_reactions (post_id, user_email, reaction_type, created_at, updated_at)
VALUES (@post_id, 'test2@haru.com', 'LIKE', NOW(), NOW());
UPDATE board_post_reactions SET reaction_type = 'LOVE', updated_at = NOW()
 WHERE post_id = @post_id AND user_email = 'test2@haru.com';

-- 카운터 갱신 (PostMapper.increaseViewCount / incrementLikeCount, CommentMapper.updatePostCommentCount)
UPDATE board_posts SET view_count = view_count + 1 WHERE id = @post_id;
UPDATE board_posts SET like_count = like_count + 1 WHERE id = @post_id;
UPDATE board_posts SET comment_count = 2 WHERE id = @post_id;
UPDATE board_comments SET reply_count = 1 WHERE id = @comment_id;

-- LocationMapper.saveLocation (chatroom 이 필요하므로 먼저 product+chatroom 생성)
INSERT INTO Products (product_code, title, description, price, email, category_id, hobby_id, transaction_type, registration_type, days, is_visible, created_at, updated_at)
VALUES ('P-TEST-1', '테스트 상품', '설명', 1000, 'test1@haru.com', 1, 2, '대면', '판매', '월,수,금', 1, NOW(), NOW());
SET @product_id = LAST_INSERT_ID();
INSERT INTO chatrooms (chatname, product_id, request_email, status, created_at, updated_at)
VALUES ('테스트 채팅방', @product_id, 'test2@haru.com', 'ACTIVE', NOW(), NOW());
SET @chatroom_id = LAST_INSERT_ID();

INSERT INTO locations (chatroom_id, email, latitude, longitude, timestamp, created_at, updated_at)
VALUES (@chatroom_id, 'test1@haru.com', 37.56600000, 126.97800000, NOW(), NOW(), NOW());

-- UserMapper.updateProfileImagePath (결손 컬럼)
UPDATE Users SET profile_image_path = '/uploads/profile/1.png', last_update_date = NOW()
 WHERE email = 'test1@haru.com';

-- UserMapper.updateUserAccountInfoStatus (테이블만 존재하면 0행 갱신 성공)
UPDATE User_Account_info SET account_status = 'Withdrawal' WHERE email = 'test1@haru.com';

SELECT '--- BoardMapper.findAllBoards ---' AS step;
SELECT b.*, u.nickname AS host_name,
       (SELECT COUNT(*) FROM board_members WHERE board_id = b.id AND status = 'ACTIVE') AS member_count
  FROM boards b LEFT JOIN Users u ON b.host_email = u.email
 WHERE b.status != 'DELETED' ORDER BY b.created_at DESC;

SELECT '--- PostMapper.getPostById ---' AS step;
SELECT p.*, b.name AS board_name, u.name AS author_name, u.nickname AS author_nickname,
       u.profile_image_path AS author_profile_image
  FROM board_posts p LEFT JOIN boards b ON p.board_id = b.id
       LEFT JOIN Users u ON p.author_email = u.email
 WHERE p.id = @post_id AND p.is_deleted = false;

SELECT '--- CommentMapper.getCommentsByPostId ---' AS step;
SELECT c.id, c.content, c.depth,
       (SELECT COUNT(*) FROM board_comments WHERE parent_id = c.id AND is_deleted = false) AS reply_count
  FROM board_comments c LEFT JOIN Users u ON c.author_email = u.email
 WHERE c.post_id = @post_id AND c.parent_id IS NULL AND c.is_deleted = false
 ORDER BY c.created_at DESC;

SELECT '--- PostMapper.findPostsWithFilters (정렬+페이징) ---' AS step;
SELECT p.id, p.title, p.view_count FROM board_posts p
       LEFT JOIN boards b ON p.board_id = b.id
       LEFT JOIN Users u ON p.author_email = u.email
 WHERE p.board_id = @board_id AND p.is_deleted = false
   AND (p.title LIKE CONCAT('%', '첫', '%') OR p.content LIKE CONCAT('%', '첫', '%'))
 ORDER BY p.view_count DESC LIMIT 10 OFFSET 0;

SELECT '--- PostReactionMapper.getReactionStatistics ---' AS step;
SELECT reaction_type AS 'type', COUNT(*) AS 'count' FROM board_post_reactions
 WHERE post_id = @post_id GROUP BY reaction_type;

SELECT '--- PostMapper.getPostImagesByPostId ---' AS step;
SELECT id, post_id AS postId, image_url AS imageUrl, created_at AS createdAt
  FROM board_post_images WHERE post_id = @post_id ORDER BY created_at ASC;

SELECT '--- BoardMemberMapper.findMembersByBoardId ---' AS step;
SELECT bm.id, bm.user_email, bm.role, bm.status, bm.joined_at, bm.invited_by,
       u.name AS user_name, u.nickname AS user_nickname, u.profile_image_path AS user_profile_image
  FROM board_members bm LEFT JOIN Users u ON bm.user_email = u.email
 WHERE bm.board_id = @board_id
 ORDER BY CASE WHEN bm.role = 'ADMIN' THEN 0 ELSE 1 END, bm.id;

SELECT '--- LocationMapper.getLastLocation ---' AS step;
SELECT l.*, u.nickname AS user_nickname FROM locations l JOIN Users u ON l.email = u.email
 WHERE l.chatroom_id = @chatroom_id AND l.email = 'test1@haru.com'
 ORDER BY l.timestamp DESC LIMIT 1;

SELECT '--- LocationMapper.getRecentLocations (GROUP BY + HAVING) ---' AS step;
SELECT l.*, u.nickname AS user_nickname FROM locations l JOIN Users u ON l.email = u.email
 WHERE l.chatroom_id = @chatroom_id AND l.timestamp >= DATE_SUB(NOW(), INTERVAL 5 MINUTE)
 GROUP BY l.email, l.location_id, l.chatroom_id, l.latitude, l.longitude,
          l.timestamp, l.created_at, l.updated_at, u.nickname
HAVING l.timestamp = MAX(l.timestamp)
 ORDER BY l.timestamp DESC;

SELECT '--- 소프트 삭제 경로 (deletePost / deleteComment) ---' AS step;
UPDATE board_posts SET is_deleted = true, deleted_at = NOW() WHERE id = @post_id AND author_email = 'test1@haru.com';
UPDATE board_comments SET is_deleted = true, deleted_at = NOW() WHERE id = @comment_id AND author_email = 'test2@haru.com';
SELECT ROW_COUNT() AS last_update_rows;

SELECT '=== ALL MAPPER QUERIES OK ===' AS result;
