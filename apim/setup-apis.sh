#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
#  Crea, despliega, publica y suscribe las APIs de Pedidos360 en WSO2
#  API Manager, a partir de las definiciones en apim/apis/*.json.
#  Correr DESPUES de apim/setup-keymanager.sh.
#
#  Es idempotente: si una API ya existe, la borra y la vuelve a crear.
#
#  Requiere: WSO2 API Manager arriba (https://localhost:9443),
#  curl y jq.
# ─────────────────────────────────────────────────────────────
set -euo pipefail

APIM_HOST="${APIM_HOST:-https://localhost:9443}"
APIM_ADMIN_USER="${APIM_ADMIN_USER:-admin}"
APIM_ADMIN_PASS="${APIM_ADMIN_PASS:-admin}"
KM_NAME="MicrosoftEntraExternalID"
APP_NAME="Pedidos360Front"
# El 'aud' del idToken de Microsoft: el Key Manager lo usa como consumer key.
MICROSOFT_CLIENT_ID="${MICROSOFT_CLIENT_ID:-7b48efc0-534c-4b13-9e02-4c60a129f151}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "== 1. Registrando client dinamico (DCR) =="
DCR_RESPONSE=$(curl -sk -X POST "${APIM_HOST}/client-registration/v0.17/register" \
  -H "Content-Type: application/json" \
  -u "${APIM_ADMIN_USER}:${APIM_ADMIN_PASS}" \
  -d '{
        "callbackUrl": "https://localhost:9443/apim",
        "clientName": "pedidos360_setup_apis",
        "owner": "'"${APIM_ADMIN_USER}"'",
        "grantType": "client_credentials password",
        "saasApp": true
      }')
CLIENT_ID=$(echo "$DCR_RESPONSE" | jq -r '.clientId')
CLIENT_SECRET=$(echo "$DCR_RESPONSE" | jq -r '.clientSecret')

echo "== 2. Pidiendo access token =="
TOKEN_RESPONSE=$(curl -sk -X POST "${APIM_HOST}/oauth2/token" \
  -u "${CLIENT_ID}:${CLIENT_SECRET}" \
  -d "grant_type=password&username=${APIM_ADMIN_USER}&password=${APIM_ADMIN_PASS}&scope=apim:api_view apim:api_create apim:api_publish apim:api_delete apim:subscribe apim:app_manage apim:sub_manage")
ADMIN_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token')

if [ -z "$ADMIN_TOKEN" ] || [ "$ADMIN_TOKEN" = "null" ]; then
  echo "No se pudo obtener el access token. Respuesta de WSO2:"
  echo "$TOKEN_RESPONSE"
  exit 1
fi

AUTH=(-H "Authorization: Bearer ${ADMIN_TOKEN}")
PUBLISHER="${APIM_HOST}/api/am/publisher/v4"
DEVPORTAL="${APIM_HOST}/api/am/devportal/v3"

# Si la Application ya existia (corrida anterior), se borra primero: asi se
# van tambien sus suscripciones, que si no bloquean el borrado de las APIs.
EXISTING_APP=$(curl -sk "${AUTH[@]}" "${DEVPORTAL}/applications?query=${APP_NAME}" \
  | jq -r --arg n "$APP_NAME" '.list[]? | select(.name == $n) | .applicationId')
if [ -n "$EXISTING_APP" ]; then
  echo "-- Application ${APP_NAME} ya existia, se borra para recrearla --"
  curl -sk -X DELETE "${AUTH[@]}" "${DEVPORTAL}/applications/${EXISTING_APP}" > /dev/null
fi

# Crea la API, la despliega en el Gateway y la publica. Imprime solo el id
# por stdout (los mensajes van a stderr para no mezclarse con el id).
create_publish_api() {
  local file="$1"
  local name existing_id body response api_id revision_id
  name=$(jq -r '.name' "$file")

  existing_id=$(curl -sk "${AUTH[@]}" "${PUBLISHER}/apis?query=name:${name}" \
    | jq -r --arg n "$name" '.list[]? | select(.name == $n) | .id')
  if [ -n "$existing_id" ]; then
    echo "-- ${name} ya existia, se borra para recrearla --" >&2
    curl -sk -X DELETE "${AUTH[@]}" "${PUBLISHER}/apis/${existing_id}" > /dev/null
  fi

  echo "-- Creando API ${name} --" >&2
  # _comentario es documentacion del JSON: la Publisher API lo rechaza.
  body=$(jq 'del(._comentario)' "$file")
  response=$(curl -sk -X POST "${PUBLISHER}/apis" "${AUTH[@]}" \
    -H "Content-Type: application/json" -d "$body")
  api_id=$(echo "$response" | jq -r '.id // empty')
  if [ -z "$api_id" ]; then
    echo "Fallo al crear ${name}. Respuesta:" >&2
    echo "$response" >&2
    return 1
  fi

  # En WSO2 4.x una API no llega al Gateway hasta desplegar una revision.
  echo "-- Desplegando ${name} en el Gateway --" >&2
  response=$(curl -sk -X POST "${PUBLISHER}/apis/${api_id}/revisions" "${AUTH[@]}" \
    -H "Content-Type: application/json" -d '{ "description": "setup-apis.sh" }')
  revision_id=$(echo "$response" | jq -r '.id // empty')
  if [ -z "$revision_id" ]; then
    echo "Fallo al crear la revision de ${name}. Respuesta:" >&2
    echo "$response" >&2
    return 1
  fi
  curl -sk -X POST "${PUBLISHER}/apis/${api_id}/deploy-revision?revisionId=${revision_id}" "${AUTH[@]}" \
    -H "Content-Type: application/json" \
    -d '[{ "name": "Default", "vhost": "localhost", "displayOnDevportal": true }]' > /dev/null

  echo "-- Publicando ${name} --" >&2
  curl -sk -X POST "${PUBLISHER}/apis/change-lifecycle?apiId=${api_id}&action=Publish" "${AUTH[@]}" > /dev/null

  echo "$api_id"
}

API_IDS=()
for file in "${SCRIPT_DIR}"/apis/*.json; do
  API_IDS+=("$(create_publish_api "$file")")
done

# Orden importante: suscribir ANTES de asociar las claves. Con claves de un
# Key Manager externo asociadas, WSO2 intenta leerlas del IdP al suscribir y
# falla (Microsoft no expone esa API). Por eso la Application se arma de cero
# en este orden (la vieja se borro al principio).
echo "== 3. Application '${APP_NAME}' en el DevPortal =="
APP_ID=$(curl -sk -X POST "${DEVPORTAL}/applications" "${AUTH[@]}" \
  -H "Content-Type: application/json" \
  -d '{
        "name": "'"${APP_NAME}"'",
        "throttlingPolicy": "Unlimited",
        "description": "Representa al SPA de Angular consumiendo las APIs a traves del Gateway"
      }' | jq -r '.applicationId')
echo "applicationId=${APP_ID}"

echo "== 4. Suscribiendo las APIs a la Application =="
for API_ID in "${API_IDS[@]}"; do
  SUB_RESPONSE=$(curl -sk -X POST "${DEVPORTAL}/subscriptions" "${AUTH[@]}" \
    -H "Content-Type: application/json" \
    -d '{ "applicationId": "'"${APP_ID}"'", "apiId": "'"${API_ID}"'", "throttlingPolicy": "Unlimited" }')
  if [ "$(echo "$SUB_RESPONSE" | jq -r '.subscriptionId // empty')" = "" ]; then
    echo "No se pudo suscribir la API ${API_ID}. Respuesta de WSO2:"
    echo "$SUB_RESPONSE"
    exit 1
  fi
  echo "-- $(echo "$SUB_RESPONSE" | jq -r '.apiInfo.name'): $(echo "$SUB_RESPONSE" | jq -r '.status')"
done

# El Gateway acepta un JWT solo si su consumer key (claim 'aud' = client id de
# Microsoft) esta asociado a una Application suscripta a la API. Como WSO2 no
# emite las claves, se asocian las existentes ("map keys").
echo "== 5. Asociando el client id de Microsoft a la Application =="
MAP_RESPONSE=$(curl -sk -X POST "${DEVPORTAL}/applications/${APP_ID}/map-keys" "${AUTH[@]}" \
  -H "Content-Type: application/json" \
  -d '{
        "consumerKey": "'"${MICROSOFT_CLIENT_ID}"'",
        "consumerSecret": "no-usado",
        "keyType": "PRODUCTION",
        "keyManager": "'"${KM_NAME}"'"
      }')
if [ "$(echo "$MAP_RESPONSE" | jq -r '.consumerKey // empty')" = "" ]; then
  echo "No se pudo asociar el client id. Respuesta de WSO2:"
  echo "$MAP_RESPONSE"
  exit 1
fi
echo "consumerKey=${MICROSOFT_CLIENT_ID}"

echo
echo "Listo. Las APIs quedaron desplegadas y PUBLISHED en el Gateway:"
echo "  http://localhost:8280/api/me                -> auth-service"
echo "  http://localhost:8280/api/inventory/*       -> inventory-service"
echo "  http://localhost:8280/api/orders/*          -> orders-service"
echo "  http://localhost:8280/api/admin/rabbitmq/*  -> rabbitmq-admin-service"
echo
echo "El token que hay que mandar en el header Authorization es el idToken que el"
echo "frontend ya obtiene al loguearse con Microsoft (no hace falta generar keys de WSO2)."
