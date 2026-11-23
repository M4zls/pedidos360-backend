# Pedidos360 — Servicio de Analitica (Kafka Streams)

**Spring Boot 4 / Java 21**. Sin base propia: el unico estado que mantiene es
el que guarda Kafka Streams localmente (RocksDB), reconstruible en cualquier
momento reprocesando los topicos de entrada. Puerto **8086**.

A diferencia de `OrderEventConsumer`/`InventoryEventConsumer` en
orders/inventory-service (consumidores normales con `@KafkaListener`, que
solo loguean cada mensaje por separado), este servicio usa la **API de Kafka
Streams** para cruzar y agregar eventos de ambos topicos entre si. Ver
`messaging.kafka.topology.OrderFulfillmentTopology`.

## Que hace la topologia

1. **`orderFulfillmentPipeline`** — join con ventana de tiempo (5 minutos,
   configurable) entre `orders.events` (evento `CREATED`) e
   `inventory.events` (evento `STOCK_RESERVED`), cruzados por `orderId`.
   Publica el resultado a **`order-fulfillment.events`**: cuanto tardo en
   reservarse el stock de cada linea de un pedido desde que se creo.

   `inventory.events` llega con clave `productId` (lo pone asi
   `InventoryEventProducer` en inventory-service); la topologia la
   reparticiona por `orderId` (`selectKey`) antes del join — Kafka Streams
   crea solo un topico interno de repartition para eso.

2. **`ordersPerMinute`** — cuenta pedidos creados en ventanas de 1 minuto
   (configurable), materializado en un state store local. Se consulta por
   API (interactive query), no por Kafka: no hace falta leer un topico para
   saber el valor actual.

## Endpoints

| Metodo | Ruta                          | Descripcion                                              |
| ------ | ----------------------------- | --------------------------------------------------------- |
| `GET`  | `/api/analytics/orders-per-minute` | Ultimos 15 minutos de conteo de pedidos creados, por ventana. |

Sin autenticacion (a diferencia del resto de los servicios): solo expone
conteos agregados, pensado para la demo. Si se necesitara protegerlo mas
adelante, el patron a copiar es el de `kafka-admin-service`
(`MultiIssuerAuthenticationManagerResolver` + rol ADMIN).

## Ejecutar

```bash
mvn spring-boot:run        # http://localhost:8086
```

Necesita el cluster de Kafka levantado (`../docker-compose.kafka.yml`) y que
orders-service/inventory-service hayan publicado al menos un evento en cada
topico — si no, los endpoints responden vacio, no fallan.

## Configuracion (`src/main/resources/application.yml`)

- `app.kafka.bootstrap-servers` — mismo cluster que orders/inventory-service.
- `app.kafka.topics.*` — nombres de `orders.events`/`inventory.events` (los
  produce cada servicio) y de `order-fulfillment.events` (lo produce este).
- `app.kafka.join-window-ms` — ventana maxima entre pedido creado y stock
  reservado para considerarlos el mismo flujo.
- `app.kafka.orders-per-minute-window-ms` — tamano de la ventana del conteo.
