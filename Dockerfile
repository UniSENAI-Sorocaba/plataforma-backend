# Etapa 1: compila o projeto com Maven.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Etapa 2: imagem final, só com o Java e o .jar.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/plataforma-backend-*.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
