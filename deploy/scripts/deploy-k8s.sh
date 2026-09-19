#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
#  Deploy del backend Pedidos360 sobre el cluster k3s de la EC2.
#  Lo invoca el workflow de GitHub Actions por SSH, despues de que las
#  imagenes ya fueron construidas y subidas a ECR por el runner.
#
#  Variables de entorno que espera:
#    IMAGE_AUTH IMAGE_INVENTORY IMAGE_ORDERS   URIs completas de ECR (con tag)
#    MICROSOFT_CLIENT_ID LOCAL_JWT_SECRET CORS_ALLOWED_ORIGIN
#
#  Uso: <este script> <directorio con los manifiestos k8s>
# ─────────────────────────────────────────────────────────────
set -euo pipefail

MANIFEST_DIR="${1:?Falta el directorio de manifiestos}"
NS=pedidos360

: "${IMAGE_AUTH:?}" "${IMAGE_INVENTORY:?}" "${IMAGE_ORDERS:?}"
: "${MICROSOFT_CLIENT_ID:?}" "${LOCAL_JWT_SECRET:?}" "${CORS_ALLOWED_ORIGIN:?}"

export KUBECONFIG="${KUBECONFIG:-$HOME/.kube/config}"

echo "[deploy] Namespace..."
kubectl apply -f "$MANIFEST_DIR/00-namespace.yaml"

# El token de ECR vence a las 12 h. El timer lo renueva solo, pero lo forzamos
# aca para que un deploy nunca dependa de cuando corrio el timer por ultima vez.
echo "[deploy] Renovando credenciales de ECR..."
sudo systemctl start pedidos360-ecr-refresh.service

echo "[deploy] Secret de configuracion de la app..."
kubectl -n "$NS" create secret generic pedidos360-secrets \
  --from-literal=MICROSOFT_CLIENT_ID="$MICROSOFT_CLIENT_ID" \
  --from-literal=LOCAL_JWT_SECRET="$LOCAL_JWT_SECRET" \
  --from-literal=CORS_ALLOWED_ORIGIN="$CORS_ALLOWED_ORIGIN" \
  --dry-run=client -o yaml | kubectl apply -f -

# Los manifiestos versionados llevan placeholders en vez de tags, asi quedan
# legibles en el repo y el tag concreto (el SHA del commit) entra recien aca.
echo "[deploy] Aplicando manifiestos..."
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
for f in "$MANIFEST_DIR"/*.yaml; do
  sed -e "s|__IMAGE_AUTH__|$IMAGE_AUTH|g" \
      -e "s|__IMAGE_INVENTORY__|$IMAGE_INVENTORY|g" \
      -e "s|__IMAGE_ORDERS__|$IMAGE_ORDERS|g" \
      "$f" > "$TMP/$(basename "$f")"
done
kubectl apply -f "$TMP"

echo "[deploy] Esperando el rollout..."
# Recreate deja el servicio caido unos segundos: el timeout contempla el
# arranque frio de Spring Boot mas el pull de la imagen nueva.
for d in auth inventory orders; do
  kubectl -n "$NS" rollout status "deployment/$d" --timeout=5m
done

echo "[deploy] Estado:"
kubectl -n "$NS" get pods,svc,ingress -o wide

echo "[deploy] OK."
