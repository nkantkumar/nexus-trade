# Multi-stage Dockerfile for Nexus Trade

# Stage 1: Build & Package
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and build executable jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal Runtime
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Install curl for container healthchecks
RUN apt-get update && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Run as dedicated unprivileged user for container security
RUN groupadd -r nexustrade && useradd -r -g nexustrade -m -d /home/nexustrade nexustrade

# Copy built artifact from builder stage
COPY --from=builder /build/target/nexus-trade-1.0.0.jar /app/nexus-trade.jar
RUN chown -R nexustrade:nexustrade /app

USER nexustrade

# Application port
EXPOSE 8081

# JVM flags tailored for container support, low-latency Aeron/Chronicle Queue IPC, and memory limits
ENV JAVA_OPTS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED \
  --add-opens=java.base/java.nio=ALL-UNNAMED \
  --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
  --add-opens=java.base/java.lang=ALL-UNNAMED \
  --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
  --add-opens=java.base/java.util=ALL-UNNAMED \
  --add-opens=java.base/java.util.concurrent=ALL-UNNAMED \
  -Dio.aeron.driver.mediaDriverDir=/dev/shm"

HEALTHCHECK --interval=10s --timeout=5s --start-period=40s --retries=5 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/nexus-trade.jar"]
