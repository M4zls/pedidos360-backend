# Pedidos360 — Servicio de Auth

**Spring Boot 4 / Java 21**. Login y roles. Proceso y base propios
(`pedidos360-auth.db`, SQLite). Puerto **8080**.

Ve también [`../inventory-service`](../inventory-service) y
[`../orders-service`](../orders-service) — cada microservicio de este proyecto
tiene su base y su README.

| Paquete                   | Qué es                                                                                   |
| ------------------------- | -------------------------------------------------------------------------------------- |
| `com.pedidos360.auth`     | Resource server: valida ID tokens de Microsoft (Entra External ID / CIAM). Unico proveedor de login. |
| `com.pedidos360.identity` | Guarda nombre/consentimiento y una copia local del rol. Convierte el JWT validado en authorities `ROLE_*`. |

## Roles

El rol (ADMIN / OPERADOR / CLIENTE) **sale del claim `roles` del JWT de
Microsoft** — son las *App Roles* asignadas en el Portal de Azure, no una
lista de emails en el código. Si una cuenta no tiene ninguna App Role
reconocida asignada, el login se **rechaza** (401) — no hay rol por defecto.

### Cómo asignar el rol a una cuenta, en Azure Portal

1. **Crear las App Roles** (una sola vez): *Microsoft Entra ID* → *App
   registrations* → tu app (`proyecto3602`, client id
   `7b48efc0-534c-4b13-9e02-4c60a129f151`) → **App roles** → *Create app
   role*, una por cada rol:

   | Display name | **Value** (esto es lo que importa) | Allowed member types |
   | --- | --- | --- |
   | Administrador | `Admin` | Users/Groups |
   | Operador      | `Operador` | Users/Groups |
   | Cliente       | `Cliente` | Users/Groups |

   El **Value** tiene que ser exactamente uno de estos (no distingue
   mayúsculas): `Admin`/`Administrador`, `Operador`, `Cliente` — es lo que
   el backend compara en `RoleJwtAuthenticationConverter.roleFromClaim`.

2. **Asignar cada cuenta a su rol**: *Microsoft Entra ID* → *Enterprise
   applications* → la misma app → **Users and groups** → *Add user/group* →
   elegís la cuenta y el rol (`pedro.porro` → Administrador, `juanfaure` →
   Operador, `raul.perez` → Cliente, etc.).

3. Para que el rol aparezca en el **ID token** (no solo en el access token),
   confirmá que el *App registration* → **Token configuration** no esté
   restringiendo el claim `roles` — por defecto, con App roles creadas, ya
   viaja en el id_token.

4. El usuario tiene que **cerrar sesión y volver a loguearse** después de
   que le asignes/cambies el rol — el token viejo no se actualiza solo.

`inventory-service` y `orders-service` validan el mismo token de forma
**independiente** (no llaman a este servicio): cada uno tiene su propia copia
de `RoleJwtAuthenticationConverter`, pero los tres leen el mismo claim, así
que no hace falta duplicar configuración de roles en ningún `application.yml`.

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
en esta carpeta (gitignoreado); cada cuenta se guarda ahí en su primer login,
con el rol que traiga el token en ese momento. Para empezar de cero, borrá
ese archivo. Para usar otra base, pasá `DB_URL`.

## Endpoints

| Método   | Ruta                  | Descripción                                              |
| -------- | --------------------- | -------------------------------------------------------- |
| `GET`    | `/api/me`             | Datos del usuario autenticado (requiere `Bearer`).        |
| `POST`   | `/api/me/consent`     | Registra el consentimiento de datos del usuario logueado.  |

## Configuración (`src/main/resources/application.yml`)

- `spring.datasource.url` — `jdbc:sqlite:pedidos360-auth.db` (override con `DB_URL`).
- `app.auth.microsoft.client-id` — debe coincidir con el front y con
  `inventory-service` / `orders-service`.
- Los roles **no se configuran acá** — se asignan en Azure Portal (ver
  sección "Roles" arriba).
