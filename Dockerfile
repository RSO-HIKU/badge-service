FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy JAR
COPY target/badge-service-0.1.0.jar ./badge-service.jar

# Copy configuration
COPY src/main/resources/config.yaml ./config.yaml

# Expose port as defined in config.yaml
EXPOSE 8087

# Run JAR with explicit config
CMD ["java", "-jar", "badge-service.jar"]
