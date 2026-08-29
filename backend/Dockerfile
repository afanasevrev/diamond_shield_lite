# Первый этап: сборка Spring Boot приложения
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Копируем pom.xml отдельно, чтобы Docker кэшировал зависимости
COPY pom.xml .

RUN mvn dependency:go-offline -B

# Копируем исходный код
COPY src ./src

# Собираем JAR
RUN mvn clean package -DskipTests -B


# Второй этап: запуск приложения
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Создаём непривилегированного пользователя
RUN addgroup -S diamond \
    && adduser -S diamond -G diamond

COPY --from=builder /build/target/*.jar /app/application.jar

RUN chown -R diamond:diamond /app

USER diamond

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/application.jar"]