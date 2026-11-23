# ─────────────────────────────────────────────────────────────
#  Pedidos360 — Servicio de analitica (Kafka Streams)
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

# RocksDB (lo usa Kafka Streams para los state stores) es una libreria nativa
# (JNI) compilada contra glibc/libstdc++; sin este paquete falla al arrancar
# con UnsatisfiedLinkError "Error loading shared library libstdc++.so.6" en
# esta imagen Alpine (musl, sin libstdc++ por defecto).
RUN apk add --no-cache libstdc++

COPY --from=build /app/target/*.jar app.jar

# Estado local de Kafka Streams (RocksDB): no hace falta persistirlo entre
# reinicios del contenedor, se reconstruye solo reprocesando orders.events e
# inventory.events desde el offset que corresponda.
RUN mkdir -p /tmp/kafka-streams && chown -R app:app /tmp/kafka-streams /app

USER app
EXPOSE 8086
# -Djava.net.preferIPv4Stack=true: sin esto, la JVM intenta conectar por el
# AAAA que devuelve el DNS de Microsoft y falla con "Network unreachable"
# (la red de Docker aca no tiene ruta IPv6).
ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-jar", "app.jar"]
