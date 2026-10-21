#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
#  Registra en WSO2 API Manager un "Key Manager" que apunta al
#  tenant de Microsoft Entra External ID (CIAM) del proyecto.
#
#  Esto es lo que le permite al Gateway de WSO2 validar la firma,
#  el issuer y la audiencia de los JWT que ya emite Microsoft
#  (el mismo idToken que usa el frontend hoy), SIN que WSO2 emita
#  tokens propios: solo verifica los que llegan por Authorization.
#
#  WSO2 4.3 no trae un tipo "solo validar JWT": se usa el conector
#  Forgerock con creacion de apps y generacion de tokens apagadas, asi
#  que sus credenciales/endpoints propios no se usan nunca (van con
#  valores de relleno). La validacion usa solo issuer + JWKS + claim.
#  Se eligio Forgerock porque al asociar claves existentes ("map keys")
#  no consulta al IdP; KeyCloak, AzureAD y PingFederate si lo hacen y
#  fallan contra Microsoft.
#
#  WSO2 carga los Key Managers en memoria al arrancar: un KM creado
#  por REST no queda disponible para las APIs hasta reiniciar. Por eso
#  el script reinicia el contenedor apim al final.
#
#  Requiere: WSO2 API Manager arriba y accesible en
#  https://localhost:9443 (docker compose up apim), curl y jq.
#  Correr desde la raiz del repo (usa docker compose para reiniciar).
# ─────────────────────────────────────────────────────────────
set -euo pipefail

APIM_HOST="${APIM_HOST:-https://localhost:9443}"
APIM_ADMIN_USER="${APIM_ADMIN_USER:-admin}"
APIM_ADMIN_PASS="${APIM_ADMIN_PASS:-admin}"
KM_NAME="MicrosoftEntraExternalID"

MICROSOFT_TENANT="${MICROSOFT_TENANT:-c450c5ae-3cee-44d6-b081-b1d7b50eaf5d}"
MICROSOFT_CLIENT_ID="${MICROSOFT_CLIENT_ID:-7b48efc0-534c-4b13-9e02-4c60a129f151}"
CIAM_BASE="https://${MICROSOFT_TENANT}.ciamlogin.com/${MICROSOFT_TENANT}"

echo "== 1. Registrando client dinamico (DCR) para poder llamar a la REST Admin API =="
DCR_RESPONSE=$(curl -sk -X POST "${APIM_HOST}/client-registration/v0.17/register" \
  -H "Content-Type: application/json" \
  -u "${APIM_ADMIN_USER}:${APIM_ADMIN_PASS}" \
  -d '{
        "callbackUrl": "https://localhost:9443/apim",
        "clientName": "pedidos360_admin_script",
        "owner": "'"${APIM_ADMIN_USER}"'",
        "grantType": "client_credentials password",
        "saasApp": true
      }')

CLIENT_ID=$(echo "$DCR_RESPONSE" | jq -r '.clientId')
CLIENT_SECRET=$(echo "$DCR_RESPONSE" | jq -r '.clientSecret')

if [ -z "$CLIENT_ID" ] || [ "$CLIENT_ID" = "null" ]; then
  echo "No se pudo registrar el client dinamico. Respuesta de WSO2:"
  echo "$DCR_RESPONSE"
  exit 1
fi

echo "== 2. Pidiendo access token para usar la REST Admin API =="
TOKEN_RESPONSE=$(curl -sk -X POST "${APIM_HOST}/oauth2/token" \
  -u "${CLIENT_ID}:${CLIENT_SECRET}" \
  -d "grant_type=password&username=${APIM_ADMIN_USER}&password=${APIM_ADMIN_PASS}&scope=apim:admin")

ADMIN_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token')

if [ -z "$ADMIN_TOKEN" ] || [ "$ADMIN_TOKEN" = "null" ]; then
  echo "No se pudo obtener el access token. Respuesta de WSO2:"
  echo "$TOKEN_RESPONSE"
  exit 1
fi

echo "== 3. Registrando el Key Manager '${KM_NAME}' =="
KM_BODY=$(jq -n \
  --arg name "$KM_NAME" \
  --arg base "$CIAM_BASE" \
  '{
     name: $name,
     displayName: "Microsoft Entra External ID (CIAM)",
     type: "Forgerock",
     description: "Valida JWT emitidos por el tenant CIAM de Pedidos360. No emite tokens: solo verifica los que llegan del frontend.",
     enabled: true,
     issuer: ($base + "/v2.0"),
     tokenEndpoint: ($base + "/oauth2/v2.0/token"),
     authorizeEndpoint: ($base + "/oauth2/v2.0/authorize"),
     clientRegistrationEndpoint: "https://no-usado.invalid/register",
     introspectionEndpoint: "https://no-usado.invalid/introspect",
     revokeEndpoint: "https://no-usado.invalid/revoke",
     consumerKeyClaim: "aud",
     scopesClaim: "scp",
     certificates: { type: "JWKS", value: ($base + "/discovery/v2.0/keys") },
     tokenValidation: [ { id: 0, enable: true, type: "JWT", value: "" } ],
     enableOAuthAppCreation: false,
     enableMapOAuthConsumerApps: true,
     enableTokenGeneration: false,
     enableTokenEncryption: false,
     enableTokenHashing: false,
     availableGrantTypes: ["authorization_code", "implicit"],
     additionalProperties: { client_id: "no-usado", client_secret: "no-usado" }
   }')

EXISTING_ID=$(curl -sk "${APIM_HOST}/api/am/admin/v4/key-managers" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  | jq -r --arg n "$KM_NAME" '.list[]? | select(.name == $n) | .id')

if [ -n "$EXISTING_ID" ]; then
  echo "-- Ya existia (id=${EXISTING_ID}), se actualiza --"
  KM_RESPONSE=$(curl -sk -X PUT "${APIM_HOST}/api/am/admin/v4/key-managers/${EXISTING_ID}" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "$KM_BODY")
else
  KM_RESPONSE=$(curl -sk -X POST "${APIM_HOST}/api/am/admin/v4/key-managers" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "$KM_BODY")
fi

if [ "$(echo "$KM_RESPONSE" | jq -r '.id // empty')" = "" ]; then
  echo "No se pudo registrar el Key Manager. Respuesta de WSO2:"
  echo "$KM_RESPONSE"
  exit 1
fi
echo "Key Manager registrado (id=$(echo "$KM_RESPONSE" | jq -r '.id'))."

echo "== 4. Reiniciando WSO2 para que cargue el Key Manager =="
docker compose restart apim
echo -n "Esperando a que WSO2 vuelva a arrancar"
sleep 20
until curl -sk -o /dev/null -w "%{http_code}" "${APIM_HOST}/api/am/publisher/v4/settings" | grep -qE "^(200|401)$"; do
  echo -n "."
  sleep 5
done
echo

echo
echo "Listo. Siguiente paso: bash apim/setup-apis.sh"
echo "Client id esperado como audiencia ('aud') de los JWT: ${MICROSOFT_CLIENT_ID}"
