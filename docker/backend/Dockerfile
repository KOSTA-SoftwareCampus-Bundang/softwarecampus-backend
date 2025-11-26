# ==============================================================================
# [Backend Dockerfile]
#
# 목적: Spring Boot 애플리케이션을 위한 경량화된 Docker 이미지 생성
# 기반 이미지: Eclipse Temurin JDK 17 (Alpine Linux)
# ==============================================================================

# 1. Base Image 설정 (Java 17, Alpine Linux)
FROM eclipse-temurin:17-jdk-alpine

# 2. 임시 파일 저장을 위한 볼륨 설정
# Tomcat의 내장 톰캣이 사용하는 임시 디렉토리
VOLUME /tmp

# 3. 빌드된 JAR 파일 위치 지정 (Maven 빌드 결과물)
ARG JAR_FILE=target/*.jar

# 4. JAR 파일을 컨테이너 내부로 복사
COPY ${JAR_FILE} app.jar

# 5. 애플리케이션 실행
# java -jar /app.jar 명령어로 실행
ENTRYPOINT ["java","-jar","/app.jar"]
