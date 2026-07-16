---
name: haru-db
description: Haru MySQL(haru_db)에 접속해 스키마/데이터를 확인한다. DB 접속, 테이블 확인, 쿼리, 스키마 점검 요청 시 사용.
---

# Haru DB 접속 & 스키마

`haru_db` @ `localhost:3306`, 계정 `root`. **비밀번호는 절대 하드코딩하지 말고** `CoreService/.env`의 `DB_PASSWORD`에서 읽는다.

## 접속 (자격증명은 .env에서만)
```bash
cd /Users/jamie/Desktop/Project/MainProject_Haru
export MYSQL_PWD="$(grep '^DB_PASSWORD=' CoreService/.env | cut -d= -f2-)"
mysql -u root haru_db -e "SHOW TABLES;"
```
특수문자(`@` 등)가 들어가도 `MYSQL_PWD` env로 넘기면 안전. 작업 후 `unset MYSQL_PWD`.

## 도메인별 주요 테이블 (40+ 테이블, 5도메인)
- **User**: `Users`, `user_hobbies`, `hobbies`, `categories`, `category_hobbies`, `UserLocation`
- **Market**: `Products`, `ProductImages`, `Transactions`, `Payments`
- **Board**: `boards`, `Posts`, `Comments`, (PostImages, PostReactions)
- **Chat**: `chatrooms`, `messages`
- **Assist**: `chat_messages` (AI 챗봇 히스토리 — `ChatMessageMapper.xml`가 참조)

> 테이블 케이싱 혼재(`Users` vs `boards`). 물리 리네임은 위험하므로 코드 레벨 정합으로만 다룸.

## 자주 쓰는 점검
```bash
mysql -u root -e "USE haru_db; DESCRIBE Users;"      # MYSQL_PWD export 후
mysql -u root -e "USE haru_db; SELECT COUNT(*) FROM chat_messages;"
mysqladmin -u root ping            # mysqld is alive
```

## 주의
- 자격증명은 `.env`에서만 읽고 커밋/하드코딩 금지.
- 운영 연결 증거는 서비스 로그의 HikariCP `Added connection com.mysql.cj.jdbc.ConnectionImpl` 라인으로 확인.
