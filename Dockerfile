# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests clean package

FROM eclipse-temurin:21-jre
WORKDIR /app

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy JAR
COPY --from=build /workspace/target/badge-service-0.1.0.jar ./badge-service.jar

# Copy configuration
COPY src/main/resources/config.yaml ./config.yaml

# Expose port as defined in config.yaml
EXPOSE 8087

# Run JAR with explicit config
CMD ["java", "-jar", "badge-service.jar"]
