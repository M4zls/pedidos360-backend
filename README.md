# Pedidos360 — Backend

**Spring Boot 4 / Java 17**. Un solo proceso con dos features separadas por paquete:

| Paquete                   | Qué es                                                                                   |
| ------------------------- | -------------------------------------------------------------------------------------- |
| `com.pedidos360.auth`     | Resource server: valida ID tokens de Microsoft (Azure AD, multi-tenant) y un JWT propio del login usuario/contraseña de ejemplo. |
| `com.pedidos360.inventory`| Microservicio de inventario: catálogo de productos y movimientos de stock (JPA / PostgreSQL). |

Backend del proyecto Pedidos360 — repo aparte del frontend
(`M4zls/pedidos360-frontend`). El `app.auth.microsoft.client-id` de acá y el
`MICROSOFT_CLIENT_ID` del front **tienen que ser el mismo valor**.

> El código conserva además soporte para ID tokens de Google
> (`MultiIssuerAuthenticationManagerResolver`), aunque el frontend actual ya no
> ofrece ese login.

## Ejecutar

```bash
docker compose up -d       # PostgreSQL en localhost:5432 (ver docker-compose.yml)
mvn spring-boot:run        # http://localhost:8080
mvn test                   # tests (usan H2 embebida, no necesitan Postgres)
mvn clean package          # jar en target/
```

Sin `docker compose`, apuntá a tu propia Postgres con las variables
`DB_URL` / `DB_USERNAME` / `DB_PASSWORD`.

## Endpoints

### Auth (`com.pedidos360.auth`)

| Método | Ruta              | Descripción                                              |
| ------ | ----------------- | ------------------------------------------------------ |
| `POST` | `/api/auth/login` | Login demo usuario/contraseña; devuelve un JWT HS256.   |
| `GET`  | `/api/me`         | Datos del usuario autenticado (requiere `Bearer` token).|

### Inventario (`com.pedidos360.inventory`) — todo requiere `Bearer` token

| Método   | Ruta                                          | Descripción                                    |
| -------- | --------------------------------------------- | --------------------------------------------- |
| `GET`    | `/api/inventory/products`                     | Lista de productos activos (`?category=`).      |
| `GET`    | `/api/inventory/products/{id}`                | Un producto.                                    |
| `POST`   | `/api/inventory/products`                     | Alta de producto → 201.                         |
| `PUT`    | `/api/inventory/products/{id}`                | Edita nombre, categoría, precio, mínimo, activo.|
| `DELETE` | `/api/inventory/products/{id}`                | Baja lógica (`active = false`) → 204.           |
| `GET`    | `/api/inventory/low-stock`                    | Productos en o bajo su umbral de reposición.    |
| `GET`    | `/api/inventory/products/{id}/movements`      | Historial de movimientos de stock.             |
| `POST`   | `/api/inventory/products/{id}/movements`      | Registra `IN` / `OUT` / `ADJUSTMENT` → 201.     |

El stock **no se edita directo**: se registra un movimiento y el servicio
recalcula el stock del producto en la misma transacción.

## Arquitectura del inventario (capa de repositorio)

```
web/ (controllers + DTOs + ExceptionHandler)
      │  usa
service/ (ProductService, InventoryService — reglas de negocio, @Transactional)
      │  usa
repository/ (ProductRepository, StockMovementRepository — Spring Data JPA)
      │  mapea
domain/ (Product, StockMovement, MovementType — entidades JPA)
```

Los controllers nunca tocan los repositorios: pasan por la capa de servicio.

## Configuración (`src/main/resources/application.yml`)

- `spring.datasource.*` — Postgres (con defaults para `docker compose`).
- `spring.jpa.hibernate.ddl-auto` — `update` para la demo; en serio, Flyway.
- `data.sql` — productos de ejemplo (idempotente, `ON CONFLICT DO NOTHING`).
- `app.auth.microsoft.client-id` — debe coincidir con el front.
- `app.auth.local.*` — usuario/contraseña y `jwt-secret` de demo (**no producción**).

## API Gateway

No hay servicio de gateway. El enrutamiento `/api/*` lo hace **nginx** en el repo
del front (`nginx.conf.template`): separa `/api/auth`, `/api/me` e
`/api/inventory`. Hoy todo apunta a este mismo backend; cuando inventario se
separe en su propio proceso, se cambia solo esa config.
