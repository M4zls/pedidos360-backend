# Pedidos360 — Servicio de Pedidos

**Spring Boot 4 / Java 21**. Microservicio con **proceso y base de datos
propios** (`pedidos360-orders.db`, SQLite). Corre en el puerto **8081**.

Pedido = cliente + líneas (producto, cantidad, precio congelado) + estado:
`PENDIENTE → EN_PREPARACION → LISTO → DESPACHADO → ENTREGADO` / `CANCELADO`.

## Cómo se integra

- **Auth**: valida los mismos Bearer tokens de Microsoft que
  [`../auth-service`](../auth-service) (unico proveedor de login), de forma
  independiente. Config en `app.auth.*` — tiene que coincidir.
- **Roles**: siempre por email con `app.roles.*` (copia de la config de
  auth-service).
- **Inventario**: NO comparte base ni repositorios con
  [`../inventory-service`](../inventory-service). Para validar productos y
  congelar precios llama por HTTP a `GET {inventory.base-url}/api/inventory/products/{id}`
  reenviando el Bearer del usuario (`InventoryClient`). El pedido **no descuenta
  stock**.

## Endpoints (`/api/orders/**`, requieren `Bearer`)

| Método  | Ruta                       | Quién          | Descripción                                    |
| ------- | -------------------------- | -------------- | --------------------------------------------- |
| `POST`  | `/api/orders`              | autenticado    | Crea un pedido (valida productos vs inventario).|
| `GET`   | `/api/orders/mine`         | autenticado    | Pedidos del usuario logueado.                   |
| `GET`   | `/api/orders?status=`      | ADMIN/OPERADOR | Todos los pedidos.                              |
| `GET`   | `/api/orders/{id}`         | dueño o staff  | Un pedido.                                      |
| `PATCH` | `/api/orders/{id}/status`  | ADMIN/OPERADOR | Cambia el estado.                              |
| `POST`  | `/api/orders/{id}/cancel`  | dueño o staff  | Cancela (el cliente solo si sigue PENDIENTE).   |

Si el inventario no responde → `502`.

## Ejecutar

```bash
mvn spring-boot:run     # http://localhost:8081  (necesita inventory-service en :8083)
mvn test                # smoke test con H2
mvn clean package       # jar en target/
```

Config (`src/main/resources/application.yml`, todo overrideable por env):
`SERVER_PORT`, `DB_URL`, `INVENTORY_BASE_URL`, `MICROSOFT_CLIENT_ID`,
`CORS_ALLOWED_ORIGIN`.
