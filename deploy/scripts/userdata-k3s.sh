#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
#  User data de la EC2 que hospeda el cluster (Amazon Linux 2023 |
#  Ubuntu 22.04/24.04). Pegar en: Launch instance -> Advanced -> User data.
#
#  Deja lista la instancia con:
#    - k3s single-node (Kubernetes + Traefik como Ingress en :80)
#    - AWS CLI v2 (usa el rol de la instancia: LabInstanceProfile)
#    - kubeconfig utilizable por el usuario de la AMI (para el deploy por SSH)
#    - un systemd timer que renueva el token de ECR cada 6 h
#
#  REQUISITO: la instancia tiene que arrancar con el IAM instance profile
#  "LabInstanceProfile" adjunto, o el AWS CLI no tendra credenciales y el
#  pull desde ECR fallara con "no basic auth credentials".
# ─────────────────────────────────────────────────────────────
set -euxo pipefail
export DEBIAN_FRONTEND=noninteractive
# El user data corre con un PATH minimo; k3s y el AWS CLI se instalan acá.
export PATH="/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:$PATH"

# Usuario "humano" de la AMI (ec2-user en Amazon Linux, ubuntu en Ubuntu).
APP_USER="$(ls /home 2>/dev/null | head -1)"
[ -n "$APP_USER" ] || APP_USER=ec2-user

. /etc/os-release
case "$ID" in
  amzn)
    # NO pedir "curl" aca: AL2023 trae curl-minimal preinstalado y el paquete
    # curl completo CONFLICTA con el. dnf aborta ("conflicts with curl provided
    # by curl-minimal") y, con set -e, se cae todo el bootstrap antes de k3s.
    # curl-minimal ya provee /usr/bin/curl, que es lo unico que necesitamos.
    dnf install -y tar unzip
    ;;
  ubuntu | debian)
    apt-get update -y
    apt-get install -y curl tar unzip ca-certificates
    ;;
  *)
    echo "SO no soportado por este userdata: $ID" >&2
    exit 1
    ;;
esac

# Chequeo explicito: si algo falta, que el error diga cual y no reviente 40
# lineas mas abajo con un mensaje incomprensible.
for cmd in curl tar unzip; do
  command -v "$cmd" >/dev/null 2>&1 || { echo "Falta $cmd" >&2; exit 1; }
done

# --- AWS CLI v2 -------------------------------------------------------------
# Lo necesita el refresco del token de ECR. La AMI de Amazon Linux ya lo trae;
# en Ubuntu hay que instalarlo.
if ! command -v aws >/dev/null 2>&1; then
  curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-$(uname -m).zip" -o /tmp/awscliv2.zip
  unzip -q -o /tmp/awscliv2.zip -d /tmp
  /tmp/aws/install --update
  rm -rf /tmp/aws /tmp/awscliv2.zip
fi

# --- k3s --------------------------------------------------------------------
# Single-node: el mismo host es control-plane y worker. Trae Traefik como
# Ingress controller y local-path como StorageClass por defecto, que es lo
# que usan los PVC de las SQLite.
# INSTALL_K3S_SKIP_SELINUX_RPM: el instalador intenta bajar el paquete
# k3s-selinux de un repo que no publica build para AL2023 y ahi se traba.
# AL2023 arranca con SELinux en modo permissive, asi que saltear esa politica
# no cambia nada funcional.
curl -sfL https://get.k3s.io | INSTALL_K3S_SKIP_SELINUX_RPM=true sh -
systemctl enable --now k3s

# Esperar a que la API responda antes de seguir (el resto depende de kubectl).
for _ in $(seq 1 60); do
  k3s kubectl get --raw='/readyz' >/dev/null 2>&1 && break
  sleep 5
done

# kubeconfig para el usuario de la AMI: asi el deploy por SSH usa kubectl
# directamente, sin sudo y sin aflojar los permisos de /etc/rancher.
install -d -m 0700 -o "$APP_USER" -g "$APP_USER" "/home/$APP_USER/.kube"
install -m 0600 -o "$APP_USER" -g "$APP_USER" \
  /etc/rancher/k3s/k3s.yaml "/home/$APP_USER/.kube/config"
grep -q 'KUBECONFIG' "/home/$APP_USER/.bashrc" 2>/dev/null || \
  echo 'export KUBECONFIG=$HOME/.kube/config' >> "/home/$APP_USER/.bashrc"

# k3s instala su propio kubectl embebido; este symlink lo deja como "kubectl".
ln -sf /usr/local/bin/k3s /usr/local/bin/kubectl

mkdir -p /opt/pedidos360
chown -R "$APP_USER":"$APP_USER" /opt/pedidos360

# --- Refresco del token de ECR ---------------------------------------------
# El token de ECR dura 12 h, asi que lo renovamos cada 6. Sin esto, el primer
# pull tras reiniciar la instancia (o pasadas 12 h) falla con ImagePullBackOff.
cat > /usr/local/bin/pedidos360-ecr-refresh.sh <<'SCRIPT'
#!/usr/bin/env bash
set -euo pipefail
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
export PATH="/usr/local/bin:$PATH"

# Region desde IMDSv2 (no la hardcodeamos: el Learner Lab suele ser us-east-1
# pero la instancia sabe la suya).
TOKEN="$(curl -sf -X PUT http://169.254.169.254/latest/api/token \
  -H 'X-aws-ec2-metadata-token-ttl-seconds: 300')"
REGION="$(curl -sf -H "X-aws-ec2-metadata-token: $TOKEN" \
  http://169.254.169.254/latest/meta-data/placement/region)"
ACCOUNT="$(aws sts get-caller-identity --query Account --output text)"
REGISTRY="${ACCOUNT}.dkr.ecr.${REGION}.amazonaws.com"

# El namespace puede no existir todavia si el timer corre antes del primer
# deploy; crearlo aca hace que el arranque no dependa del orden.
kubectl create namespace pedidos360 --dry-run=client -o yaml | kubectl apply -f -

PASSWORD="$(aws ecr get-login-password --region "$REGION")"
kubectl -n pedidos360 create secret docker-registry ecr-creds \
  --docker-server="$REGISTRY" \
  --docker-username=AWS \
  --docker-password="$PASSWORD" \
  --dry-run=client -o yaml | kubectl apply -f -

echo "[ecr-refresh] secret ecr-creds actualizado para $REGISTRY"
SCRIPT
chmod 0755 /usr/local/bin/pedidos360-ecr-refresh.sh

cat > /etc/systemd/system/pedidos360-ecr-refresh.service <<'UNIT'
[Unit]
Description=Renueva el secret de pull de ECR en el cluster k3s
After=k3s.service
Wants=k3s.service

[Service]
Type=oneshot
ExecStart=/usr/local/bin/pedidos360-ecr-refresh.sh
UNIT

cat > /etc/systemd/system/pedidos360-ecr-refresh.timer <<'UNIT'
[Unit]
Description=Renueva el token de ECR cada 6 h (el token vive 12 h)

[Timer]
OnBootSec=2min
OnUnitActiveSec=6h
Persistent=true

[Install]
WantedBy=timers.target
UNIT

systemctl daemon-reload
systemctl enable --now pedidos360-ecr-refresh.timer
# Primera ejecucion inmediata para no esperar a OnBootSec.
systemctl start pedidos360-ecr-refresh.service || \
  echo "AVISO: el primer refresco de ECR fallo. Falta LabInstanceProfile?"

echo "Bootstrap listo (SO=$ID, usuario=$APP_USER, k3s + ECR refresh activos)."
