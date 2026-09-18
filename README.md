# Pedidos360 — Backend

**Spring Boot 4 / Java 21.** Tres microservicios independientes, cada uno con
su **proceso, base SQLite y puerto propios**. Ninguno comparte tablas ni
repositorios con otro; se hablan por HTTP cuando lo necesitan.

| Servicio                                | Puerto | Base                      | Qué hace                                              |
| ---------------------------------------- | ------ | -------------------------- | ------------------------------------------------------ |
| [`auth-service`](auth-service)           | 8080   | `pedidos360-auth.db`       | Login (Microsoft) y roles.        |
| [`inventory-service`](inventory-service) | 8083   | `pedidos360-inventory.db`  | Catálogo de productos y stock.                          |
| [`orders-service`](orders-service)       | 8081   | `pedidos360-orders.db`     | Pedidos. Consulta `inventory-service` por HTTP.         |

Cada carpeta tiene su propio `pom.xml`, `Dockerfile` y `README.md` con el
detalle de endpoints y configuración.

## Por qué están separados así

Los tres validan **el mismo Bearer token de Microsoft** (unico proveedor de
login), cada uno de forma independiente — no hay un servicio central de
autorización. El rol de cada request se resuelve por email contra una copia
de `app.roles` en cada `application.yml`. Por eso, si cambiás `app.roles` o
`app.auth.microsoft.client-id`, hay que actualizarlo en **los tres**.

El tenant de Microsoft (`proyecto3602.onmicrosoft.com`) es un tenant **Entra
External ID (CIAM)**, no Entra ID "workforce" clásico: cada servicio valida el
issuer del ID token contra `https://<tenant>.ciamlogin.com/<tenant>/v2.0`
(`MultiIssuerAuthenticationManagerResolver`), no contra
`login.microsoftonline.com`. Si algún día se cambia a un tenant Entra ID
normal, ese patrón hay que actualizarlo en los tres servicios.

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

## Cuentas

El rol es fijo por email (`app.roles`, copiado en los tres `application.yml`)
— no hay endpoint para cambiarlo en runtime.

| Login                                                   | Rol      |
| -------------------------------------------------------- | -------- |
| Microsoft — pedro.porro@proyecto3602.onmicrosoft.com      | ADMIN    |
| Microsoft — juanfaure@proyecto3602.onmicrosoft.com        | OPERADOR |
| Microsoft — cualquier otra cuenta del tenant              | CLIENTE  |

Detalle completo en [`auth-service/README.md`](auth-service/README.md).
