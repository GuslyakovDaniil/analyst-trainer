# Stage 1: Сборка проекта
FROM maven:3.8.8-eclipse-temurin-17 AS build
COPY . /app
WORKDIR /app
RUN mvn clean package -DskipTests

# Stage 2: Запуск
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /app/target/analyst-trainer-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-cp", "app.jar", "com.example.Main"]