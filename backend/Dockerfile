# ---- Stage 1: Build ----
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

# Copy Gradle wrapper and config first (layer caching for dependencies)
COPY gradlew settings.gradle build.gradle ./
COPY gradle/ gradle/

# Make gradlew executable and download dependencies
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# Copy source code
COPY src/ src/

# Build the fat JAR (skip tests — they run separately)
RUN ./gradlew bootJar --no-daemon

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
