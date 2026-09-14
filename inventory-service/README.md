# Pedidos360 — Servicio de Inventario

**Spring Boot 4 / Java 17**. Catálogo de productos y movimientos de stock.
Proceso y base propios (`pedidos360-inventory.db`, SQLite). Puerto **8083**.

## Cómo se integra

- **Auth**: valida los mismos Bearer tokens que [`../auth-service`](../auth-service)
  (Microsoft multi-tenant + JWT del login local HS256), de forma independiente
  — no llama al servicio de auth. Config en `app.auth.*`, tiene que coincidir.
- **Roles**: del claim `roles` del token; si no viene (Microsoft sin App
  Roles), por email con `app.roles.*` (copia de la config de auth-service).
  Leer el catálogo requiere sesión; alta/edición/baja/movimientos requieren
  ADMIN u OPERADOR.
- **Pedidos**: [`orders-service`](../orders-service) le consulta productos por
  HTTP para validar y congelar precios. Este servicio no sabe nada de pedidos.

## Endpoints (`/api/inventory/**`, requieren `Bearer`)

| Método   | Ruta                                          | Quién          | Descripción                                    |
| -------- | --------------------------------------------- | -------------- | ----------------------------------------------- |
| `GET`    | `/api/inventory/products`                     | autenticado    | Lista de productos activos (`?category=`).       |
| `GET`    | `/api/inventory/products/{id}`                | autenticado    | Un producto.                                     |
| `POST`   | `/api/inventory/products`                     | ADMIN/OPERADOR | Alta de producto → 201.                          |
| `PUT`    | `/api/inventory/products/{id}`                | ADMIN/OPERADOR | Edita nombre, categoría, precio, mínimo, activo. |
| `DELETE` | `/api/inventory/products/{id}`                | ADMIN/OPERADOR | Baja lógica (`active = false`) → 204.            |
| `GET`    | `/api/inventory/low-stock`                    | autenticado    | Productos en o bajo su umbral de reposición.     |
| `GET`    | `/api/inventory/products/{id}/movements`      | autenticado    | Historial de movimientos de stock.               |
| `POST`   | `/api/inventory/products/{id}/movements`      | ADMIN/OPERADOR | Registra `IN` / `OUT` / `ADJUSTMENT` → 201.      |

El stock **no se edita directo**: se registra un movimiento y el servicio
recalcula el stock del producto en la misma transacción.

## Arquitectura (capa de repositorio)

```
controller/ (ProductController, StockMovementController, DTOs, ExceptionHandler)
      │  usa
service/    (ProductService, InventoryService — reglas de negocio, @Transactional)
      │  usa
repository/ (ProductRepository, StockMovementRepository — Spring Data JPA)
      │  mapea
domain/     (Product, StockMovement, MovementType — entidades JPA)
```

Los controllers nunca tocan los repositorios: pasan por la capa de servicio.

## Ejecutar

```bash
mvn spring-boot:run     # http://localhost:8083
mvn test                # tests (repositorio con H2, contexto completo)
mvn clean package       # jar en target/
```

Usa **SQLite**: al arrancar crea `pedidos360-inventory.db` en esta carpeta y
la siembra con la carta de ejemplo (`InventoryDataInitializer`, con
`imageUrl`). Para regenerarla, borrá ese archivo.

Config (`src/main/resources/application.yml`, todo overrideable por env):
`SERVER_PORT`, `DB_URL`, `MICROSOFT_CLIENT_ID`, `LOCAL_JWT_SECRET`,
`LOCAL_JWT_AUDIENCE`, `CORS_ALLOWED_ORIGIN`, `app.roles.*`.
