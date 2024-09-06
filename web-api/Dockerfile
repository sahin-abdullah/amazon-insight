# Stage 1: Build (using JDK)
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app
COPY ./mvnw ./mvnw
COPY ./.mvn ./.mvn
COPY ./pom.xml ./pom.xml
COPY ./src ./src
RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests

# Stage 2: Run (using JRE)
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/amazon-insight-app.jar .
# Create a user and group
RUN groupadd -r spring && useradd -r -g spring spring
# Change ownership of the work directory
RUN chown -R spring:spring /app
USER spring
CMD ["java", "-jar", "/app/amazon-insight-app.jar"]