# --- Stage 1: Build Layer ---
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# 1. Copy the root/parent POM from the root context
COPY pom.xml .

# 2. Copy the individual module folder directories
COPY url-shortener-service ./url-shortener-service
COPY url-redirect-service ./url-redirect-service

# 3. Build only the specific module using the -pl flag to prevent parent lookup errors
# (For the redirect service Dockerfile, change the -pl argument to: -pl url-redirect-service)
RUN mvn clean package -pl url-shortener-service -am -DskipTests=true

# --- Stage 2: Minimal Runtime Layer ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the generated JAR from the specific module target folder
COPY --from=builder /build/url-shortener-service/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
