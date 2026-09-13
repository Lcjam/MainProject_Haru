-- Haru 로컬 개발 시드 (schema.sql 적용 후 실행)
--
--   mysql -u root < sql/schema.sql
--   mysql -u root < sql/seed-local.sql
--
-- 구성:
--   1) 로컬 테스트 계정 2개 — 비밀번호는 아래 평문 그대로. **로컬 전용, prod 주입 금지.**
--   2) 참조 데이터 — categories 20 / hobbies 69 / category_hobbies 83 (앱 필수 마스터 데이터)
--
-- 모든 INSERT 는 IGNORE 라 재실행해도 안전하다.
--
-- 테스트 계정 (login_method=EMAIL):
--   test1@haru.com / Haru!local1   (nickname: haru_tester1)
--   test2@haru.com / Haru!local1   (nickname: haru_tester2)
-- password_hash 는 BCrypt strength 12 (CoreService PasswordUtils / SecurityConfig 와 동일).
-- 재생성:  python3 -c "import bcrypt;print(bcrypt.hashpw(b'Haru!local1',bcrypt.gensalt(12,prefix=b'2a')).decode())"

SET NAMES utf8mb4;
USE `haru_db`;

--
-- 1) 로컬 테스트 계정
--
INSERT IGNORE INTO `Users`
  (`email`, `password_hash`, `name`, `nickname`, `login_method`, `social_provider`, `account_status`, `authority`)
VALUES
  ('test1@haru.com', '$2a$12$108c5iDQSxNDIi2qWcrN0u57qVaG3jqkolxH4NUa1j6vJ.WVvo8qa', '테스터1', 'haru_tester1', 'EMAIL', 'NONE', 'Active', 'USER'),
  ('test2@haru.com', '$2a$12$108c5iDQSxNDIi2qWcrN0u57qVaG3jqkolxH4NUa1j6vJ.WVvo8qa', '테스터2', 'haru_tester2', 'EMAIL', 'NONE', 'Active', 'USER');

--
-- 2) 참조 데이터 (categories / hobbies / category_hobbies)
--
INSERT IGNORE INTO `categories` (`category_id`, `category_name`) VALUES (16,'건강과 웰빙');
INSERT IGNORE INTO `categories` (`category_id`, `category_name`) VALUES (10,'게임');
INSERT IGNORE INTO `categories` (`category_id`, `category_name`) VALUES (6,'공연 예술');
INSERT IGNORE INTO `categories` (`category_id`, `category_name`) VALUES (13,'기술과 공학');
INSERT IGNORE INTO `categories` (`category_id`, `category_name`) VALUES (20,'독서와 문학');
INSERT IGNORE INTO `categories` (`category_id`, `category_name`) VALUES (18,'반려동물');
INSERT IGNORE INTO `categories` (`category_id`, `category_name`) VALUES (17,'사회 활동');
INSERT IGNORE INTO `categories` (`category_id`, `category_name`) VALUES (19,'수공예');
INSERT IGNORE INTO `categories` (`category_id`, `category_name`) VALUES (8,'수집');
INSERT IGNORE INTO `categories` (`category_id`, `category_name`) VALUES (1,'스포츠');
INSERT IGNORE INTO `categories` (`category_id`, `category_name`) VALUES (5,'시각 예술');
INSERT IGNORE INTO `categories` (`category_id`, `category_name`) VALUES (2,'실내 운동');
INSERT IGNORE INTO `categories` (`category_id`, `category_name`) VALUES (3,'야외 활동');
INSERT IGNORE INTO `categories` (`category_id`, `category_name`) VALUES (14,'언어 학습');
INSERT IGNORE INTO `categories` (`category_id`, `category_name`) VALUES (12,'여행');
INSERT IGNORE INTO `categories` (`category_id`, `category_name`) VALUES (7,'요리와 음식');
INSERT IGNORE INTO `categories` (`category_id`, `category_name`) VALUES (4,'음악');
INSERT IGNORE INTO `categories` (`category_id`, `category_name`) VALUES (11,'자기계발');
INSERT IGNORE INTO `categories` (`category_id`, `category_name`) VALUES (15,'자연과 환경');
INSERT IGNORE INTO `categories` (`category_id`, `category_name`) VALUES (9,'창작 활동');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (41,'DIY 가구 제작');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (42,'가죽 공예');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (6,'골프');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (25,'그림 그리기');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (19,'기타 연주');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (15,'낚시');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (23,'노래');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (2,'농구');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (43,'도예');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (51,'독서');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (38,'동전 수집');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (67,'동호회 참여');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (21,'드럼');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (57,'드론 조종');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (13,'등산');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (28,'디지털 아트');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (53,'로드트립');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (56,'로봇 만들기');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (64,'마라톤');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (49,'명상');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (54,'문화 탐방');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (22,'바이올린');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (68,'반려견 훈련');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (30,'발레');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (52,'배낭여행');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (5,'배드민턴');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (34,'베이킹');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (46,'보드 게임');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (11,'복싱');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (66,'봉사 활동');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (45,'비디오 게임');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (40,'빈티지 패션 수집');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (16,'사이클링');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (26,'사진 촬영');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (18,'서핑');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (7,'수영');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (69,'수족관 관리');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (17,'스키/스노보드');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (62,'식물 키우기');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (65,'아로마테라피');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (3,'야구');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (33,'양식 요리');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (60,'언어 교환');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (29,'연극');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (35,'와인 테이스팅');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (59,'외국어 학습');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (8,'요가');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (37,'우표 수집');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (24,'작곡');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (50,'저널링');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (58,'전자기기 DIY');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (61,'정원 가꾸기');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (27,'조각');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (63,'조류 관찰');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (48,'체스');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (1,'축구');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (44,'캘리그라피');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (14,'캠핑');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (36,'커피 브루잉');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (55,'코딩');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (12,'클라이밍');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (4,'테니스');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (47,'포커');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (39,'피규어 수집');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (20,'피아노');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (9,'필라테스');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (32,'한식 요리');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (10,'헬스');
INSERT IGNORE INTO `hobbies` (`hobby_id`, `hobby_name`) VALUES (31,'현대무용');
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (1,1);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (1,2);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (1,3);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (1,4);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (1,5);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (1,6);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (1,7);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (16,7);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (2,8);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (16,8);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (2,9);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (2,10);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (2,11);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (2,12);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (3,13);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (3,14);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (3,15);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (1,16);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (3,16);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (3,17);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (3,18);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (4,19);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (4,20);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (4,21);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (4,22);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (4,23);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (4,24);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (5,25);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (9,25);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (3,26);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (5,26);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (5,27);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (5,28);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (6,29);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (6,30);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (6,31);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (7,32);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (7,33);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (7,34);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (9,34);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (7,35);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (7,36);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (8,37);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (8,38);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (8,39);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (8,40);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (9,41);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (9,42);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (19,42);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (9,43);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (19,43);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (9,44);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (19,44);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (10,45);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (10,46);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (10,47);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (10,48);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (11,49);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (16,49);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (11,50);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (20,50);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (11,51);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (20,51);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (12,52);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (12,53);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (12,54);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (11,55);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (13,55);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (13,56);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (13,57);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (13,58);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (14,59);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (14,60);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (15,61);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (15,62);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (15,63);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (16,64);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (16,65);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (17,66);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (17,67);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (17,68);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (18,68);
INSERT IGNORE INTO `category_hobbies` (`category_id`, `hobby_id`) VALUES (18,69);
