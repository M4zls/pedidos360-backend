# ─────────────────────────────────────────────────────────────
#  Pedidos360 — Servicio de inventario (Spring Boot 4 / Java 21)
#  Build multi-stage: compila con Maven y corre sobre un JRE liviano.
# ─────────────────────────────────────────────────────────────

# --- Stage 1: build ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

# --- Stage 2: runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app

COPY --from=build /app/target/*.jar app.jar

# Base SQLite propia del servicio; montar un volumen sobre /data para persistirla.
ENV DB_URL=jdbc:sqlite:/data/pedidos360-inventory.db
RUN mkdir -p /data && chown -R app:app /data /app
VOLUME ["/data"]

USER app
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
