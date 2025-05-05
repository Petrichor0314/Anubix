FROM eclipse-temurin:21-jre

WORKDIR /app

COPY api-gateway/target/app.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
