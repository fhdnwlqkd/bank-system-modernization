# 1단계: 빌드 스테이지 (Gradle 기준)
FROM gradle:8.14-jdk21 AS build
WORKDIR /app

# 그래들 의존성 먼저 복사하여 캐시 활용
COPY build.gradle settings.gradle /app/
RUN gradle build -x test --parallel --continue > /dev/null 2>&1 || true

# 소스 코드 복사 및 빌드
COPY . /app
RUN gradle clean build -x test

# 2단계: 실행 스테이지
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# 빌드 스테이지에서 생성된 jar 파일만 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 컨테이너 실행 시 사용할 환경 변수
ENV TZ=Asia/Seoul

# 포트 개방
EXPOSE 8080

# 어플리케이션 실행
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]