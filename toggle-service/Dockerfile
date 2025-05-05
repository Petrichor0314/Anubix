FROM eclipse-temurin:21-jre

WORKDIR /app

COPY toggle-service/target/app.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
