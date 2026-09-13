FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/plataforma-backend-*.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
