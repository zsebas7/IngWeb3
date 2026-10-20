# Etapa 1: compila el JAR con Maven (nadie necesita Java/Maven instalado localmente)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Primero solo el pom para cachear las dependencias entre builds
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# Etapa 2: imagen liviana solo con el JRE y el JAR
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/gaslogix.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
