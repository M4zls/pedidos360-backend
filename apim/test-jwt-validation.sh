#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
#  Evidencia para la entrega: demuestra que el Gateway de WSO2
#  rechaza peticiones sin token / con token invalido, y acepta
#  las que llevan un JWT valido de Microsoft Entra External ID.
#
#  Uso:
#    1) Login normal en el front (http://localhost:8081 en Docker,
#       o http://localhost:4200 en desarrollo).
#    2) Abrir DevTools -> Application/Storage -> Session Storage
#       -> copiar el value de la clave que contiene el idToken
#       (buscada por MSAL, con prefijo "idtoken" o dentro del
#       objeto de cache de la cuenta).
#    3) export TOKEN="<el JWT copiado>"
#    4) bash apim/test-jwt-validation.sh
# ─────────────────────────────────────────────────────────────
set -uo pipefail

GATEWAY="${GATEWAY:-http://localhost:8280}"

echo "1) Sin header Authorization -> se espera 401 (rechazado por el Gateway)"
curl -s -o /dev/null -w "   HTTP %{http_code}\n" "${GATEWAY}/api/me"

echo "2) Con un token invalido/basura -> se espera 401 (rechazado por el Gateway)"
curl -s -o /dev/null -w "   HTTP %{http_code}\n" \
  -H "Authorization: Bearer esto-no-es-un-jwt-valido" \
  "${GATEWAY}/api/me"

if [ -z "${TOKEN:-}" ]; then
  echo
  echo "3) Con un JWT valido -> OMITIDO: falta la variable TOKEN."
  echo "   Exportala con el idToken obtenido al loguearte en el front y volve a correr este script."
  exit 0
fi

echo "3) Con un JWT valido (variable \$TOKEN) -> se espera 200 (aceptado por el Gateway y reenviado a auth-service)"
curl -s -w "\n   HTTP %{http_code}\n" \
  -H "Authorization: Bearer ${TOKEN}" \
  "${GATEWAY}/api/me"
