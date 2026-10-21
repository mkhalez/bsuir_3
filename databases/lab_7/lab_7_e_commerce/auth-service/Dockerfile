FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY auth-service/pom.xml auth-service/pom.xml
COPY auth-service/src auth-service/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -f auth-service/pom.xml -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/auth-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
