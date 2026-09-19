<#
 ─────────────────────────────────────────────────────────────
  Sincroniza las credenciales temporales del AWS Academy Learner Lab.

  El Learner Lab entrega credenciales que vencen a las ~4 h, y no permite
  crear usuarios IAM ni federacion OIDC con GitHub. Asi que los secrets
  AWS_* de los dos repos hay que reescribirlos al empezar cada sesion.
  Este script hace eso de una sola pasada.

  Que hace:
    1. Lee el bloque de credenciales del lab (por defecto, del portapapeles).
    2. Lo escribe en ~/.aws/credentials (para que tu AWS CLI local funcione).
    3. Lo carga como secrets en los dos repos de GitHub, con `gh`.
    4. Verifica con sts get-caller-identity.

  Como obtener el bloque:
    En el Learner Lab -> "AWS Details" -> "AWS CLI" -> "Show". Copiar TODO
    (empieza con [default]) y correr este script.

  Requisitos: AWS CLI v2, GitHub CLI (`gh auth login` hecho).

  Uso:
    .\sync-lab-credentials.ps1                      # lee del portapapeles
    .\sync-lab-credentials.ps1 -Path creds.txt      # lee de un archivo
    .\sync-lab-credentials.ps1 -SkipGitHub          # solo credenciales locales
 ─────────────────────────────────────────────────────────────
#>
[CmdletBinding()]
param(
  [string]  $Path,
  [string[]]$Repos = @('M4zls/pedidos360-backend', 'M4zls/pedidos360-frontend'),
  [string]  $Region = 'us-east-1',
  [switch]  $SkipGitHub
)

$ErrorActionPreference = 'Stop'

# --- 1. Leer el bloque -------------------------------------------------------
if ($Path) {
  if (-not (Test-Path $Path)) { throw "No existe el archivo: $Path" }
  $raw = Get-Content -Raw -Path $Path
} else {
  $raw = Get-Clipboard -Raw
  if ([string]::IsNullOrWhiteSpace($raw)) {
    throw "El portapapeles esta vacio. Copiá el bloque de AWS Details -> AWS CLI -> Show."
  }
}

function Get-CredField([string]$text, [string]$field) {
  $m = [regex]::Match($text, "(?m)^\s*$field\s*=\s*(\S+)\s*$")
  if (-not $m.Success) {
    throw "No encontre '$field' en el bloque. Copiaste el bloque completo (empieza con [default])?"
  }
  return $m.Groups[1].Value
}

$accessKey    = Get-CredField $raw 'aws_access_key_id'
$secretKey    = Get-CredField $raw 'aws_secret_access_key'
$sessionToken = Get-CredField $raw 'aws_session_token'

Write-Host "Credenciales leidas (access key $($accessKey.Substring(0,8))...)." -ForegroundColor Green

# --- 2. Escribirlas localmente ----------------------------------------------
$awsDir = Join-Path $HOME '.aws'
if (-not (Test-Path $awsDir)) { New-Item -ItemType Directory -Path $awsDir | Out-Null }

@"
[default]
aws_access_key_id=$accessKey
aws_secret_access_key=$secretKey
aws_session_token=$sessionToken
"@ | Out-File -FilePath (Join-Path $awsDir 'credentials') -Encoding ascii

@"
[default]
region=$Region
output=json
"@ | Out-File -FilePath (Join-Path $awsDir 'config') -Encoding ascii

Write-Host "Escritas en $awsDir\credentials" -ForegroundColor Green

# --- 3. Verificar y derivar el registry -------------------------------------
$identity = aws sts get-caller-identity --output json | ConvertFrom-Json
if (-not $?) { throw "sts get-caller-identity fallo: las credenciales no sirven o ya vencieron." }

$account  = $identity.Account
$registry = "$account.dkr.ecr.$Region.amazonaws.com"
Write-Host "Cuenta $account — identidad: $($identity.Arn)" -ForegroundColor Green

# --- 4. Cargar los secrets en GitHub ----------------------------------------
if ($SkipGitHub) {
  Write-Host "`n-SkipGitHub: no se tocaron los repos." -ForegroundColor Yellow
  return
}

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
  throw "No encuentro el GitHub CLI (gh). Instalalo o usá -SkipGitHub."
}

$secrets = @{
  AWS_ACCESS_KEY_ID     = $accessKey
  AWS_SECRET_ACCESS_KEY = $secretKey
  AWS_SESSION_TOKEN     = $sessionToken
}

foreach ($repo in $Repos) {
  Write-Host "`n-> $repo" -ForegroundColor Cyan
  foreach ($name in $secrets.Keys) {
    gh secret set $name --repo $repo --body $secrets[$name]
    if ($LASTEXITCODE -ne 0) { throw "Fallo al cargar $name en $repo" }
    Write-Host "   $name (secret) actualizado"
  }

  # ECR_REGISTRY va como VARIABLE, no como secret: los workflows lo meten en un
  # output de step, y GitHub descarta los outputs que contienen un secret. La
  # URL del registry no es sensible (el account ID aparece en cada URI de ECR).
  gh variable set ECR_REGISTRY --repo $repo --body $registry
  if ($LASTEXITCODE -ne 0) { throw "Fallo al cargar ECR_REGISTRY en $repo" }
  Write-Host "   ECR_REGISTRY (variable) actualizado"
}

Write-Host "`nListo. Los secrets AWS_* vencen en ~4 h; volvé a correr esto la proxima sesion." -ForegroundColor Green
