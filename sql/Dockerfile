FROM gradle:8.10.2-jdk17-alpine AS build

WORKDIR /workspace
COPY sql/build.gradle sql/settings.gradle ./
COPY sql/src ./src
COPY sql/schema.sql ./schema.sql
COPY sql/migrations ./migrations
RUN gradle --no-daemon installDist

FROM eclipse-temurin:17-jre-alpine

WORKDIR /opt/haru-migrator
COPY --from=build /workspace/build/install/haru-database-migrations/ ./
COPY sql/schema.sql ./sql/schema.sql
COPY sql/migrations ./sql/migrations
COPY sql/docker-entrypoint.sh /usr/local/bin/haru-migrate
RUN chmod 0755 /usr/local/bin/haru-migrate

ENV HARU_SQL_DIR=/opt/haru-migrator/sql

ENTRYPOINT ["/usr/local/bin/haru-migrate"]
CMD ["auto"]
