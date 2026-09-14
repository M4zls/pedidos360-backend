# ─────────────────────────────────────────────────────────────
#  Pedidos360 — Servicio de auth + roles (Spring Boot 4 / Java 17)
#  Build multi-stage: compila con Maven y corre sobre un JRE liviano.
# ─────────────────────────────────────────────────────────────

# --- Stage 1: build ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache de dependencias: primero el pom, después el código.
COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY src ./src
RUN mvn -q clean package -DskipTests

# --- Stage 2: runtime ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Usuario sin privilegios.
RUN addgroup -S app && adduser -S app -G app

COPY --from=build /app/target/*.jar app.jar

# La base SQLite vive acá; montar un volumen sobre /data para persistirla.
ENV DB_URL=jdbc:sqlite:/data/pedidos360-auth.db
RUN mkdir -p /data && chown -R app:app /data /app
VOLUME ["/data"]

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
