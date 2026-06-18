# Multi-stage build for k8s-operator

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/
COPY user-service/pom.xml user-service/
COPY order-service/pom.xml order-service/
COPY payment-service/pom.xml payment-service/
COPY notification-service/pom.xml notification-service/
COPY ai-service/pom.xml ai-service/
COPY k8s-operator/pom.xml k8s-operator/

RUN mvn dependency:go-offline -pl k8s-operator -am -q

COPY k8s-operator/src k8s-operator/src

RUN mvn package -pl k8s-operator -am -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S icop && adduser -S icop -G icop

COPY --from=builder /build/k8s-operator/target/k8s-operator-*.jar app.jar

RUN chown icop:icop app.jar
USER icop

EXPOSE 8086
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
