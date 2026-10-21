# WSO2 API Manager — gateway centralizado con validación de JWT

Esta carpeta agrega **WSO2 API Manager** como capa intermedia entre el
frontend (via nginx) y los microservicios (`auth`, `inventory`, `orders`,
`rabbitmq-admin`). Antes, nginx reenviaba `/api/*` directo a cada backend; ahora
reenvía todo al Gateway de WSO2 (puerto `8280`), y es WSO2 quien:

1. Decide, según el path (`/api/me`, `/api/inventory/*`, `/api/orders/*`,
   `/api/admin/rabbitmq/*`), a qué microservicio reenviar la petición.
   WSO2 quita el contexto antes de reenviar, por eso el endpoint de cada
   API en `apis/*.json` incluye el prefijo real (ej.
   `http://inventory:8083/api/inventory`).
2. **Valida el JWT** (`Authorization: Bearer ...`) contra las JWKS del
   tenant de Microsoft Entra External ID (CIAM) del proyecto, sin emitir
   tokens propios. Si falta el header, el token está mal formado, la
   firma no valida o el `issuer`/`aud` no coinciden, responde `401`
   **antes** de llegar al microservicio.

Cada microservicio Spring Boot sigue validando el JWT por su cuenta (no
se tocó ese código): WSO2 es una capa adicional de validación
centralizada, no un reemplazo de la seguridad existente.

## Requisitos

- Docker Desktop corriendo.
- `curl` y `jq` disponibles en la terminal donde se corren los scripts
  (en Windows, usar Git Bash o WSL).

## Pasos

```bash
# 1. Levantar todo el stack, incluido WSO2 (tarda varios minutos la
#    primera vez: la imagen pesa ~1.5 GB y WSO2 arranca lento)
docker compose up --build

# 2. Esperar a que WSO2 responda en https://localhost:9443/carbon
#    (login admin/admin por defecto)

# 3. Registrar el Key Manager que valida los JWT de Microsoft
#    (al final reinicia el contenedor apim: WSO2 solo carga los Key
#    Managers al arrancar)
bash apim/setup-keymanager.sh

# 4. Crear, desplegar, publicar y suscribir las 4 APIs
bash apim/setup-apis.sh

# 5. Loguearse normalmente en el front (http://localhost:8081),
#    copiar el idToken desde sessionStorage (DevTools -> Application)
#    y exportarlo:
export TOKEN="<idToken copiado>"

# 6. Evidencia de aceptacion/rechazo de JWT
bash apim/test-jwt-validation.sh
```

El script de test debe mostrar:

| Caso | Resultado esperado |
|---|---|
| Sin header `Authorization` | `401` |
| Con un token basura/invalido | `401` |
| Con el `idToken` real del login | `200` (mismo body que devuelve hoy `/api/me`) |

## Sobre los contextos elegidos

Los contextos de las 3 APIs (`/api/me`, `/api/inventory`, `/api/orders`)
son **idénticos** a los prefijos que ya usaba `nginx.docker.conf`, así
el frontend no cambió ni una URL: solo cambió el destino final de esas
URLs (WSO2 en vez del backend directo).

Las rutas `/api/auth/*` y `/api/admin/*` que nginx reservaba no tienen
hoy ningún controller implementado en `auth-service` (`MeController` es
el único), así que no se creó una API de WSO2 para ellas. Si en el
futuro se agregan esos endpoints, el patrón a seguir es el mismo: nuevo
archivo en `apim/apis/`, agregarlo a `setup-apis.sh`, y el contexto debe
matchear el prefijo real.

## Desarrollo con `ng serve` (sin Docker completo)

Por defecto `npm start` usa `front/proxy.conf.bypass-wso2.json` (configurado
en `front/angular.json`), que llama directo a cada backend
(`8080`/`8083`/`8081`/`8084`) **sin pasar por WSO2** — útil para debug, pero
no cumple el requisito de la consigna de pasar por el API Manager.

Para que el front en desarrollo pase por WSO2, levantar al menos WSO2 y los
backends con Docker (`docker compose up auth inventory orders rabbitmq-admin
apim`), correr los scripts de setup y usar el proxy que apunta al Gateway
(`http://localhost:8280`):

```bash
cd front && npx ng serve --proxy-config proxy.conf.json
```

## Archivos

- `conf/deployment.toml` — config de WSO2 montada en el contenedor. Respecto
  del default de la imagen solo cambia `enable_outbound_auth_header = true`:
  el Gateway reenvía el header `Authorization` al backend (por defecto lo
  quita, y los microservicios responderían 401).
- `setup-keymanager.sh` — registra el Key Manager `MicrosoftEntraExternalID`
  (JWKS + issuer del tenant CIAM) vía la REST Admin API y reinicia WSO2.
- `apis/*.json` — definición de cada API (contexto, endpoint de backend,
  operaciones, CORS, Key Manager asociado) para la Publisher REST API.
- `setup-apis.sh` — crea, despliega (revision), publica y suscribe las APIs
  de `apis/`, y asocia el client id de Microsoft a la Application
  `Pedidos360Front`. Es idempotente: borra y recrea lo que ya exista.
- `test-jwt-validation.sh` — evidencia curl de rechazo/aceptación de JWT
  para la entrega del trabajo.

## Cómo valida WSO2 el token de Microsoft

- WSO2 4.3 no tiene un tipo de Key Manager "solo validar JWT". Se registra
  con el conector **Forgerock**, con creación de apps y emisión de tokens
  apagadas: sus credenciales y endpoints propios llevan valores de relleno y
  nunca se usan. La validación usa solo el `issuer`, las JWKS (firma) y el
  claim `aud`. Se eligió Forgerock porque al asociar claves existentes no
  consulta al IdP (KeyCloak, AzureAD y PingFederate sí, y fallan).
- El Gateway acepta un JWT solo si su `aud` (el client id de Microsoft) está
  asociado a una Application suscripta a la API. `setup-apis.sh` hace esa
  asociación ("map keys") **después** de suscribir: con la clave ya asociada,
  WSO2 intenta leerla del IdP al suscribir y falla.
- WSO2 quita el contexto antes de reenviar (`/api/inventory/products` llega
  como `/products`), por eso el endpoint de cada API incluye el prefijo real.

## Nota

Estos scripts usan la REST Admin/Publisher/DevPortal API de WSO2 API
Manager 4.3.x. Los nombres de campo son estables entre versiones 4.x,
pero si la versión de la imagen (`wso2/wso2am` en `docker-compose.yml`)
difiere, puede hacer falta ajustar algún campo puntual — si un script
falla, el mismo resultado se puede lograr a mano desde el Publisher
(`https://localhost:9443/publisher`) y el Admin Portal
(`https://localhost:9443/admin`), que es la alternativa que documenta la
consigna del trabajo como "correctamente configurado" vía UI.
