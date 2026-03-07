
# Build Stage
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy parent pom first (dependency cache)
COPY pom.xml .

# Copy module poms
COPY astraeus/pom.xml astraeus/
COPY idrac-redfish-lib/pom.xml idrac-redfish-lib/
COPY idrac-redfish-lib/idrac-backup/pom.xml idrac-redfish-lib/idrac-backup/
COPY idrac-redfish-lib/idrac-restore/pom.xml idrac-redfish-lib/idrac-restore/

# Download dependencies first (faster rebuilds)
RUN mvn -B -q -e -DskipTests dependency:go-offline

# Copy full source
COPY . .

# Build the project
RUN mvn clean package -DskipTests


# Runtime Stage

FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

# Copy only the final microservice jar
COPY --from=builder /build/astraeus/target/*.jar app.jar

# Expose application port
EXPOSE 8080

# Start Spring Boot application
ENTRYPOINT ["java","-jar","/app/app.jar"]