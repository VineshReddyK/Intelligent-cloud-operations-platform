# Multi-stage build for ai-service
# Note: runtime uses eclipse-temurin:21-jre-jammy (glibc) instead of alpine
# because DJL PyTorch native libs require glibc — musl libc (Alpine) is incompatible.

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy parent POM and all module POMs first for layer-cached dependency resolution
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/
COPY user-service/pom.xml user-service/
COPY order-service/pom.xml order-service/
COPY payment-service/pom.xml payment-service/
COPY notification-service/pom.xml notification-service/
COPY ai-service/pom.xml ai-service/

# Download ai-service and its module dependencies (cached layer)
RUN mvn dependency:go-offline -pl ai-service -am -q

# Copy source and build
COPY ai-service/src ai-service/src

RUN mvn package -pl ai-service -am -DskipTests -q

# ── Stage 2: Runtime (glibc required for PyTorch native) ──────────────────────
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN addgroup --system icop && adduser --system --ingroup icop icop

COPY --from=builder /build/ai-service/target/ai-service-*.jar app.jar

RUN chown icop:icop app.jar
USER icop

EXPOSE 8085
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
