FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build
COPY pom.xml .
COPY src ./src

RUN mvn clean package -q -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=builder /build/target/marealta.jar /app/marealta.jar

EXPOSE 7070

ENTRYPOINT ["java", "-jar", "/app/marealta.jar"]
