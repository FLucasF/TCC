FROM maven:3.9.6-eclipse-temurin-21 as builder

WORKDIR /build
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=builder /build/target/mare-alta-1.0.0.jar ./mare-alta.jar

EXPOSE 7070

CMD ["java", "-jar", "mare-alta.jar"]
