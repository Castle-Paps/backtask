# ── Etapa 1: compilar ─────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Descargar dependencias primero (aprovecha caché de Docker si pom.xml no cambia)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Compilar sin tests
COPY src ./src
RUN mvn package -DskipTests -q

# ── Etapa 2: imagen final (solo JRE alpine, ~100 MB vs ~600 MB) ───────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Carpeta para uploads (fotos/videos). Se monta como volumen en producción.
RUN mkdir -p uploads

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
