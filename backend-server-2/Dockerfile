FROM openjdk:17
WORKDIR /app
COPY target/backend-server-2-*.jar /app/backend-server-2.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "backend-server-2.jar"]