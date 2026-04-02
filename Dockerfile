FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build

# Trust Zscaler corporate proxy CA in both OS and Java trust stores
#COPY zscaler-root-ca.pem /usr/local/share/ca-certificates/zscaler-root-ca.crt
#RUN update-ca-certificates && \
#    keytool -importcert -noprompt -trustcacerts \
#      -alias zscaler-root-ca \
#      -file /usr/local/share/ca-certificates/zscaler-root-ca.crt \
#      -keystore $JAVA_HOME/lib/security/cacerts \
#      -storepass changeit
#
COPY pom.xml ./
RUN mvn dependency:go-offline -B

COPY src src
RUN mvn package -DskipTests -B && \
    mv target/bill-payment-service-*.jar target/app.jar

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre

# Create non-root user
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

WORKDIR /app

RUN chown -R appuser:appgroup /app

COPY --from=build --chown=appuser:appgroup /build/target/app.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
