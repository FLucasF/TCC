FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build
COPY . .
RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/app.jar .
ENTRYPOINT ["java", "-jar", "app.jar"]
