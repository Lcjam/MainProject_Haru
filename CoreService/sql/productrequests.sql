-- productrequests — "함께하기(공동구매/모집)" 요청 테이블
--
-- 배경: 이 테이블은 ProductMapper / ProductRequestMapper / ChatRoomMapper 전반에서
-- 참조되지만, 일부 로컬 haru_db에는 생성돼 있지 않아 채팅방 목록/상세 조회 시
-- "Table 'haru_db.productrequests' doesn't exist" (MySQL 1146) → HTTP 500이 발생했다.
-- 리포에 스키마 파일이 없어(수동 셋업) 재발 방지를 위해 매퍼에서 역도출한 DDL을 기록한다.
--
-- 적용: mysql -u root haru_db < CoreService/sql/productrequests.sql
-- 컬럼/타입은 model/Market/ProductRequest.java + mapper(productrequests 참조) 기준,
-- 스타일(엔진/charset/FK)은 기존 chatrooms 테이블과 정합.

CREATE TABLE IF NOT EXISTS `productrequests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `requester_email` varchar(255) NOT NULL,
  `status` varchar(20) DEFAULT '대기',           -- 대기 / 진행중 / 완료
  `approval_status` varchar(20) DEFAULT '미승인', -- 승인 / 미승인
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `product_id` (`product_id`),
  KEY `requester_email` (`requester_email`),
  CONSTRAINT `productrequests_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `Products` (`id`) ON DELETE CASCADE,
  CONSTRAINT `productrequests_ibfk_2` FOREIGN KEY (`requester_email`) REFERENCES `Users` (`email`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
