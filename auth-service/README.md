# Pedidos360 — Servicio de Auth

**Spring Boot 4 / Java 21**. Login y roles. Proceso y base propios
(`pedidos360-auth.db`, SQLite). Puerto **8080**.

Ve también [`../inventory-service`](../inventory-service) y
[`../orders-service`](../orders-service) — cada microservicio de este proyecto
tiene su base y su README.

| Paquete                   | Qué es                                                                                   |
| ------------------------- | -------------------------------------------------------------------------------------- |
| `com.pedidos360.auth`     | Resource server: valida ID tokens de Microsoft (Entra External ID / CIAM). Unico proveedor de login. |
| `com.pedidos360.identity` | Roles por email (ADMIN / OPERADOR / CLIENTE), fijos. Convierte el JWT validado en authorities `ROLE_*`. |

## Roles

Setup actual (tenant `proyecto3602.onmicrosoft.com`):

| Login                                         | Rol          |
| ---------------------------------------------- | ------------ |
| **Microsoft** — pedro.porro@proyecto3602.onmicrosoft.com | **ADMIN** — por email (`app.roles.admins`). |
| **Microsoft** — juanfaure@proyecto3602.onmicrosoft.com   | **OPERADOR** — por email (`app.roles.operadores`). |
| **Microsoft** — cualquier otra cuenta del tenant (ej. raul.perez@proyecto3602.onmicrosoft.com) | **CLIENTE** — default. |

El rol se resuelve **siempre por email** (el token de Microsoft no trae
ningún claim de rol propio): `app.roles.admins` / `app.roles.operadores`;
quien no figure queda **CLIENTE**. El rol es **fijo por email** y se
resincroniza en cada login: no existe ningún endpoint para cambiarlo en
runtime, así nadie puede entrar con un rol distinto al que le corresponde
por su cuenta.

`inventory-service` y `orders-service` validan el mismo token de forma
**independiente** (no llaman a este servicio): cada uno tiene su propia copia
de `RoleJwtAuthenticationConverter` + `app.roles` en su `application.yml`. Si
cambiás `app.roles` acá, actualizalo también en esos dos.

| Rol      | Puede                                                                    |
| -------- | ---------------------------------------------------------------------- |
| ADMIN    | Todo: inventario, pedidos, cocina, despacho y ventas.                    |
| OPERADOR | Gestionar inventario (POST/PUT/DELETE), cocina, despacho y estados de pedidos. |
| CLIENTE  | Ver catálogo, crear y ver/cancelar sus propios pedidos.                 |

Backend del proyecto Pedidos360 — repo aparte del frontend
(`M4zls/pedidos360-frontend`). El `app.auth.microsoft.client-id` de acá y el
`MICROSOFT_CLIENT_ID` del front **tienen que ser el mismo valor**.

## Ejecutar

```bash
mvn spring-boot:run        # http://localhost:8080
mvn test                   # tests (usan H2 en memoria)
mvn clean package          # jar en target/
```

**No necesita Docker.** Usa **SQLite**: al arrancar crea `pedidos360-auth.db`
en esta carpeta (gitignoreado) y siembra los usuarios con rol preconfigurado
(`app.roles`). Para empezar de cero, borrá ese archivo. Para usar otra base,
pasá `DB_URL`.

## Endpoints

| Método   | Ruta                  | Descripción                                              |
| -------- | --------------------- | -------------------------------------------------------- |
| `GET`    | `/api/me`             | Datos del usuario autenticado (requiere `Bearer`).        |
| `POST`   | `/api/me/consent`     | Registra el consentimiento de datos del usuario logueado.  |

## Configuración (`src/main/resources/application.yml`)

- `spring.datasource.url` — `jdbc:sqlite:pedidos360-auth.db` (override con `DB_URL`).
- `app.roles.*` — roles por email (admins/operadores).
- `app.auth.microsoft.client-id` — debe coincidir con el front y con
  `inventory-service` / `orders-service`.
