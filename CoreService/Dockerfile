# syntax=docker/dockerfile:1
# Compose(R04) 이미지: 소스에서 bootJar를 빌드한다. Redis는 별도 compose 서비스가 담당한다.
FROM gradle:8.12.1-jdk17 AS build
WORKDIR /workspace
COPY settings.gradle build.gradle ./
COPY src/main ./src/main
RUN --mount=type=cache,target=/home/gradle/.gradle/caches \
    gradle --no-daemon bootJar && cp build/libs/*.jar /workspace/app.jar

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 haru \
    && useradd --system --uid 10001 --gid haru --home-dir /data --no-create-home haru

# JAR은 볼륨(/data) 밖에 둔다. 코드가 user.dir 기준으로 업로드 경로를 만들므로 작업 디렉터리는 /data.
COPY --from=build /workspace/app.jar /opt/haru/app.jar
# 빈 uploads_data 볼륨을 처음 마운트할 때 기본 프로필 이미지가 시드되도록 이미지에 포함한다.
COPY src/main/resources/static/images/default-profile.png /data/src/main/resources/static/images/
COPY src/main/resources/static/profile-images/default-profile.png /data/src/main/resources/static/profile-images/
RUN mkdir -p /data/uploads/chat-images /data/src/main/resources/static/uploads \
    && chown -R haru:haru /data

WORKDIR /data
USER haru
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/opt/haru/app.jar"]
