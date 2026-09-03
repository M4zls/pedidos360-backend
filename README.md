# Pedidos360 — Backend

Resource server en **Spring Boot 3.3 / Java 17**. Valida los ID tokens de
Microsoft (Azure AD, multi-tenant) y un JWT propio del login usuario/contraseña
de ejemplo, y expone la identidad del usuario.

Backend del proyecto Pedidos360 — repo aparte del frontend
(`M4zls/pedidos360-frontend`). El `app.auth.microsoft.client-id` de acá y el
`MICROSOFT_CLIENT_ID` del front **tienen que ser el mismo valor**.

> El código conserva además soporte para ID tokens de Google
> (`MultiIssuerAuthenticationManagerResolver`), aunque el frontend actual ya no
> ofrece ese login.

## Ejecutar

```bash
mvn spring-boot:run        # http://localhost:8080
mvn test                   # tests
mvn clean package          # jar en target/
```

## Endpoints

| Metodo | Ruta              | Descripcion                                             |
| ------ | ----------------- | ----------------------------------------------------- |
| `POST` | `/api/auth/login` | Login de demo usuario/contraseña; devuelve un JWT HS256. |
| `GET`  | `/api/me`         | Datos del usuario autenticado (requiere `Bearer` token). |

## Estructura (`src/main/java/com/pedidos360/auth/`)

- `config/` — `SecurityConfig`: CORS, cadena de filtros y rutas publicas.
- `controller/` — endpoints REST.
- `security/` — `MultiIssuerAuthenticationManagerResolver` y `AudienceValidator`
  para aceptar varios issuers (Google / Microsoft / local) y validar el `audience`.

## Configuracion

`src/main/resources/application.yml`:

- `app.auth.google.client-id`, `app.auth.microsoft.client-id`
- `app.auth.local` — usuario/contraseña de demo (**no usar en produccion**)
- `app.auth.local.jwt-secret` — clave de firma HS256 (mover a variable de entorno)
