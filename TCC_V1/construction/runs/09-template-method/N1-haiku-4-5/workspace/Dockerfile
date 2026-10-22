FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/mare-alta.jar ./mare-alta.jar

EXPOSE 7070

ENTRYPOINT ["java", "-jar", "mare-alta.jar"]
