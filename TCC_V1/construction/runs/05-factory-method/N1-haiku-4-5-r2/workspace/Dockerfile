FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=builder /build/target/app.jar .

EXPOSE 7070

CMD ["java", "-jar", "app.jar"]
