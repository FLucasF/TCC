FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/pousada-mare-alta-1.0.0.jar app.jar

EXPOSE 7070

ENTRYPOINT ["java", "-jar", "app.jar"]
