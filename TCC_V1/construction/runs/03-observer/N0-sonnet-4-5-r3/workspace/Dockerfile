FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/reservations-1.0.0.jar app.jar

EXPOSE 7070

CMD ["java", "-jar", "app.jar"]
