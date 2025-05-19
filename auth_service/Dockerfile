FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY auth_service/target/app.jar app.jar

ENTRYPOINT ["java","-jar","app.jar"]
