FROM eclipse-temurin:21-jre-alpine
ENTRYPOINT ["java", "-jar", "app.jar"]

EXPOSE 8080

RUN mkdir -p /app/data

COPY target/bill-payments-service-*.jar app.jar

WORKDIR /app