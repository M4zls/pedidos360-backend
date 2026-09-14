# Pedidos360 — Servicio de Auth

**Spring Boot 4 / Java 17**. Login y roles. Proceso y base propios
(`pedidos360-auth.db`, SQLite). Puerto **8080**.

Ve también [`../inventory-service`](../inventory-service) y
[`../orders-service`](../orders-service) — cada microservicio de este proyecto
tiene su base y su README.

| Paquete                   | Qué es                                                                                   |
| ------------------------- | -------------------------------------------------------------------------------------- |
| `com.pedidos360.auth`     | Resource server: valida ID tokens de Microsoft (Azure AD, multi-tenant) y un JWT propio del login usuario/contraseña. Emite ese JWT (`POST /api/auth/login`). |
| `com.pedidos360.identity` | Roles por email (ADMIN / OPERADOR / CLIENTE). Convierte el JWT validado en authorities `ROLE_*` y expone `/api/admin/users`. |

## Roles

Setup actual:

| Login                        | Rol                                                        |
| ---------------------------- | -------------------------------------------------------- |
| **Microsoft** (cam.carrascop)| **ADMIN** — por email (`app.roles.admins` en `application.yml`). |
| **`operador` / `operador123`** | **OPERADOR** — usuario local (`app.auth.local.users`).     |
| **`cliente` / `cliente123`**   | **CLIENTE** — usuario local.                               |

El rol de los usuarios locales viaja en el claim **`roles`** del JWT que emite
`LocalAuthController`. Para Microsoft, como el token no trae `roles`, se resuelve
por **email**: `app.roles.admins` / `app.roles.operadores`; quien no figure queda
**CLIENTE**. Un ADMIN puede ajustar esos roles por email en runtime
(`PATCH /api/admin/users/{id}`, tabla `app_user`).

`inventory-service` y `orders-service` validan el mismo token de forma
**independiente** (no llaman a este servicio): cada uno tiene su propia copia
de `RoleJwtAuthenticationConverter` + `app.roles` en su `application.yml`. Si
cambiás `app.roles` acá, actualizalo también en esos dos.

| Rol      | Puede                                                                    |
| -------- | ---------------------------------------------------------------------- |
| ADMIN    | Todo, incluida la gestión de roles (`/api/admin/**`).                    |
| OPERADOR | Gestionar inventario (POST/PUT/DELETE) y estados de pedidos.             |
| CLIENTE  | Ver catálogo, crear y ver/cancelar sus propios pedidos.                 |

Backend del proyecto Pedidos360 — repo aparte del frontend
(`M4zls/pedidos360-frontend`). El `app.auth.microsoft.client-id` de acá y el
`MICROSOFT_CLIENT_ID` del front **tienen que ser el mismo valor**.

> El código conserva además soporte para ID tokens de Google
> (`MultiIssuerAuthenticationManagerResolver`), aunque el frontend actual ya no
> ofrece ese login.

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
| `POST`   | `/api/auth/login`     | Login usuario/contraseña; devuelve un JWT HS256.          |
| `GET`    | `/api/me`             | Datos del usuario autenticado (requiere `Bearer`).        |
| `GET`    | `/api/admin/users`    | Lista usuarios y roles (ADMIN).                            |
| `PATCH`  | `/api/admin/users/{id}` | Cambia el rol de un usuario (ADMIN).                     |

## Configuración (`src/main/resources/application.yml`)

- `spring.datasource.url` — `jdbc:sqlite:pedidos360-auth.db` (override con `DB_URL`).
- `app.roles.*` — roles por email (admins/operadores).
- `app.auth.microsoft.client-id` — debe coincidir con el front y con
  `inventory-service` / `orders-service`.
- `app.auth.local.*` — usuarios/contraseñas y `jwt-secret` de demo (**no
  producción**). El `jwt-secret` y `audience` tienen que ser iguales en los
  tres servicios (validan el mismo JWT).
