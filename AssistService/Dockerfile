# syntax=docker/dockerfile:1
# Compose(R04) 이미지: 소스에서 bootJar를 빌드한다.
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
    && useradd --system --uid 10001 --gid haru --no-create-home haru
COPY --from=build /workspace/app.jar /opt/haru/app.jar
WORKDIR /opt/haru
USER haru
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/opt/haru/app.jar"]
