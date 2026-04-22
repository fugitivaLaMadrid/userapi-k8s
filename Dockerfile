# Stage 1: Build with Maven + JDK 17
FROM maven:3.9.2-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the Spring Boot application
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=build /app/target/userapi-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]