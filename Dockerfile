# 베이스 이미지: Java 17 슬림 이미지
FROM openjdk:17-jdk-slim

# 앱 실행 경로
WORKDIR /app

# target 폴더에서 JAR 복사 (JAR 이름은 정확히 써야 함)
COPY target/toychatuser-0.0.1-SNAPSHOT.jar app.jar

# 실행 포트 (Koyeb에서는 내부적으로 알아서 라우팅함)
EXPOSE 8080

# 앱 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]