# --- Stage 1: Build Layer ---
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Compile and package application into a runnable fat JAR
RUN mvn clean package -DskipTests=true

# --- Stage 2: Minimal Runtime Layer ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create a non-root system user for security enforcement
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy only the compiled JAR from Stage 1
COPY --from=builder /build/target/*.jar app.jar

# Expose server port (will be bound dynamically by docker-compose)
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
