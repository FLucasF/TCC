FROM maven:3.9.5-eclipse-temurin-21 AS builder

WORKDIR /build
COPY pom.xml .
COPY src ./src

RUN mvn clean package -q -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=builder /build/target/app.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
