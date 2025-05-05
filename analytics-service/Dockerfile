FROM eclipse-temurin:21-jre

WORKDIR /app

COPY analytics-service/target/app.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
