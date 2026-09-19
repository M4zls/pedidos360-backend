#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
#  Crea los 4 repositorios de ECR del proyecto. Se corre UNA sola vez,
#  desde tu maquina, con las credenciales del Learner Lab activas.
#
#  Es idempotente: si el repo ya existe no falla, solo lo reporta.
#  Al final imprime el ECR_REGISTRY que va como secret en los dos repos
#  de GitHub.
#
#  Uso:  ./create-ecr-repos.sh [region]        (por defecto us-east-1)
# ─────────────────────────────────────────────────────────────
set -euo pipefail

REGION="${1:-us-east-1}"
REPOS=(pedidos360/auth pedidos360/inventory pedidos360/orders pedidos360/front)

ACCOUNT="$(aws sts get-caller-identity --query Account --output text)"
REGISTRY="${ACCOUNT}.dkr.ecr.${REGION}.amazonaws.com"

# Conservar solo las 10 imagenes mas nuevas: el credito del Learner Lab es
# finito y cada imagen de Java pesa ~200 MB.
LIFECYCLE='{"rules":[{"rulePriority":1,"description":"Conservar las ultimas 10 imagenes","selection":{"tagStatus":"any","countType":"imageCountMoreThan","countNumber":10},"action":{"type":"expire"}}]}'

for repo in "${REPOS[@]}"; do
  if aws ecr describe-repositories --region "$REGION" --repository-names "$repo" >/dev/null 2>&1; then
    echo "[=] $repo ya existe"
  else
    aws ecr create-repository \
      --region "$REGION" \
      --repository-name "$repo" \
      --image-scanning-configuration scanOnPush=true \
      --image-tag-mutability MUTABLE \
      >/dev/null
    echo "[+] $repo creado"
  fi

  aws ecr put-lifecycle-policy \
    --region "$REGION" \
    --repository-name "$repo" \
    --lifecycle-policy-text "$LIFECYCLE" \
    >/dev/null
done

echo
echo "Listo. Cargá este valor como secret ECR_REGISTRY en AMBOS repos de GitHub:"
echo "  $REGISTRY"
echo "y AWS_REGION = $REGION"
