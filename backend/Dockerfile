# syntax=docker/dockerfile:1

# Build the executable JAR with the project's Maven wrapper.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

COPY backend/.mvn/ .mvn/
COPY backend/mvnw backend/pom.xml ./
RUN sed -i 's/\r$//' mvnw \
    && chmod +x mvnw \
    && ./mvnw --batch-mode --no-transfer-progress dependency:go-offline

COPY backend/src/ src/
RUN ./mvnw --batch-mode --no-transfer-progress clean package -DskipTests

# Minimal runtime with a non-root user and a container health probe.
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/target/campus-exam-scheduler-0.0.1-SNAPSHOT.jar app.jar
USER app
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=10 \
    CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
