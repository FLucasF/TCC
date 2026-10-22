FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/app.jar /app/app.jar

EXPOSE 7070

CMD ["java", "-jar", "/app/app.jar"]
