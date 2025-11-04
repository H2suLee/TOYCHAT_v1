# 1단계: Maven 빌드용 이미지
FROM maven:3.8.6-eclipse-temurin-17 AS builder

WORKDIR /build

# 소스 복사
COPY pom.xml .
COPY src ./src

# 패키지 빌드 (테스트 생략)
RUN mvn clean package -DskipTests


# 2단계: 실행용 슬림 이미지
FROM openjdk:17.0.1-jdk-slim

WORKDIR /app

# 1단계에서 빌드한 JAR 복사
COPY --from=builder /build/target/toychatuser-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 9090

ENTRYPOINT ["java", "-jar", "app.jar"]
