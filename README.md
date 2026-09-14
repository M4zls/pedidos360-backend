# Pedidos360 — Backend

**Spring Boot 4 / Java 17.** Tres microservicios independientes, cada uno con
su **proceso, base SQLite y puerto propios**. Ninguno comparte tablas ni
repositorios con otro; se hablan por HTTP cuando lo necesitan.

| Servicio                                | Puerto | Base                      | Qué hace                                              |
| ---------------------------------------- | ------ | -------------------------- | ------------------------------------------------------ |
| [`auth-service`](auth-service)           | 8080   | `pedidos360-auth.db`       | Login (Microsoft + usuario/contraseña) y roles.        |
| [`inventory-service`](inventory-service) | 8083   | `pedidos360-inventory.db`  | Catálogo de productos y stock.                          |
| [`orders-service`](orders-service)       | 8081   | `pedidos360-orders.db`     | Pedidos. Consulta `inventory-service` por HTTP.         |

Cada carpeta tiene su propio `pom.xml`, `Dockerfile` y `README.md` con el
detalle de endpoints y configuración.

## Por qué están separados así

Los tres validan **el mismo Bearer token** (Microsoft o el JWT que emite
`auth-service`), cada uno de forma independiente — no hay un servicio central
de autorización. El rol de cada request sale del claim `roles` del token, o
(si no viene) de una copia de `app.roles` en cada `application.yml`. Por eso,
si cambiás `app.roles`, `app.auth.local.jwt-secret` o `app.auth.microsoft.client-id`,
hay que actualizarlo en **los tres**.

`orders-service` no tiene tabla de productos: cuando arma un pedido, le
pregunta a `inventory-service` por HTTP (`GET /api/inventory/products/{id}`)
para validar el producto y congelar el precio.

## Levantar los tres en desarrollo

```bash
cd auth-service      && mvn spring-boot:run   # :8080
cd inventory-service  && mvn spring-boot:run   # :8083
cd orders-service     && mvn spring-boot:run   # :8081
```

El front (`../front`, `npm start`) ya sabe enrutar cada `/api/*` al puerto que
corresponde (`proxy.conf.json`). En producción lo hace nginx
(`../front/nginx.conf`). Con Docker, `../docker-compose.yml` levanta los tres
más el front.

## Cuentas de la demo

| Login                          | Rol      |
| ------------------------------- | -------- |
| Microsoft (cam.carrascop)       | ADMIN    |
| `operador` / `operador123`      | OPERADOR |
| `cliente` / `cliente123`        | CLIENTE  |

Detalle completo en [`auth-service/README.md`](auth-service/README.md).
