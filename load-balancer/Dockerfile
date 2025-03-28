FROM openjdk:17
WORKDIR /app
COPY target/load-balancer-*.jar load-balancer.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "load-balancer.jar"]
