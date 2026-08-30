FROM eclipse-temurin:25-jre
WORKDIR app
COPY target/payment-service-0.0.1-SNAPSHOT.jar payment-service.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "payment-service.jar"]