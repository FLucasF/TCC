FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/reservations-1.0-SNAPSHOT.jar app.jar
EXPOSE 7070
CMD ["java", "-jar", "app.jar"]
