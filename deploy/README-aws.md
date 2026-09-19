# Despliegue en AWS — ECR + Kubernetes (k3s) sobre EC2

Pipeline **gitflow → GitHub Actions → ECR → k3s**. El runner compila las
imágenes, las publica en **Amazon ECR**, y una EC2 con **k3s** (Kubernetes
certificado, distribución liviana de Rancher) las descarga y las corre.

```
                     ┌─ push a develop ─→ build + push a ECR (sin desplegar)
gitflow ─ GH Actions ┤
                     └─ push a main ────→ build + push a ECR ─ SSH ─→ kubectl apply
                                                                         │
navegador ──:80──→ EC2 (Elastic IP) ──→ Traefik Ingress ─────────────────┤
                                          ├─ /                → front    │
                                          ├─ /api/me          → auth     │ k3s
                                          ├─ /api/inventory   → inventory│
                                          └─ /api/orders      → orders   ┘
```

## Por qué k3s y no EKS

La cuenta es un **AWS Academy Learner Lab**, y EKS ahí es inviable:

| Obstáculo | Detalle |
| --- | --- |
| Roles IAM | EKS necesita un cluster role y un node role. El Learner Lab no permite `iam:CreateRole`, y por eso `eksctl` falla siempre (crea roles vía CloudFormation). |
| SCP sobre `eks:*` | En la mayoría de las versiones del lab el servicio directamente no está habilitado. |
| Presupuesto | El control plane de EKS cuesta **$0.10/h (~$73/mes)** y **sigue facturando con el lab detenido**. Con nodos, el crédito se consume en 2-3 semanas. |
| Nodos efímeros | El lab apaga las EC2 al cerrar sesión, así que los worker nodes se caen solos. |

k3s da el mismo modelo de objetos (Deployment, Service, Ingress, PVC, Secret) en
una sola EC2, sin costo extra. **Los manifiestos de `k8s/` son portables tal cual
a EKS**: si algún día tenés una cuenta sin restricciones, cambiás el kubeconfig y
se aplican igual.

> Si querés confirmarlo vos mismo, con las credenciales del lab activas:
> `aws eks list-clusters --region us-east-1`. Un `AccessDeniedException` que
> menciona una SCP cierra el tema.

## Paso 1 — La instancia EC2

1. `EC2 → Launch instance`:
   - **AMI**: Amazon Linux 2023 (o Ubuntu 22.04/24.04; el user data detecta el SO).
   - **Tipo**: **`c7i-flex.large`** (2 vCPU, 4 GB). Ver
     [Elegir el tipo de instancia](#elegir-el-tipo-de-instancia): con menos de
     4 GB los pods no entran.
   - **Red**: la **VPC default**, en cualquiera de sus subredes, con
     *Auto-assign public IP* en **Enable**. No armes una VPC propia (ver abajo).
   - **Key pair**: el que AWS te da al lanzar la instancia; es **tu** acceso
     personal por SSH. La clave que usa el pipeline es **otra** — ver
     [Clave SSH del runner](#clave-ssh-del-runner).
   - **IAM instance profile**: **`LabInstanceProfile`** ← *sin esto el pull desde
     ECR falla con `no basic auth credentials`*. Es el paso que más se olvida.
   - **User data**: pegar `deploy/scripts/userdata-k3s.sh` **completo y textual**,
     empezando por la línea `#!/usr/bin/env bash` (sin línea en blanco antes: ese
     shebang es lo que hace que cloud-init lo ejecute). Dejá **destildada** la
     casilla *"User data has already been base64 encoded"*.

     > Corre **una sola vez, en el primer arranque**, como root — no se repite en
     > los start/stop que hace el lab entre sesiones. Tarda unos minutos: si
     > entrás por SSH y `kubectl` todavía no existe, esperá. Para confirmar que
     > terminó (por SSH o por **Session Manager**, da igual — en SSM ya tenés
     > `sudo` sin contraseña):
     >
     > ```bash
     > sudo cloud-init status --wait
     > sudo tail -20 /var/log/cloud-init-output.log   # buscá "Bootstrap listo"
     > ```
     >
     > Si olvidaste pegarlo, no alcanza con reiniciar: copiá el script a la
     > instancia y corré `sudo bash userdata-k3s.sh`.
2. **Security Group**:

   | Puerto | Origen | Para qué |
   | --- | --- | --- |
   | `80` | `0.0.0.0/0` | Traefik: el SPA y la API |
   | `22` | `0.0.0.0/0` | SSH: el deploy del runner y el tuyo |

   Ya **no** hace falta abrir 8080/8081/8083: el tráfico entra por el Ingress y
   viaja dentro del cluster.

   > **Por qué el `22` va abierto y no restringido a tu IP.** El runner de GitHub
   > Actions no conecta desde tu IP, sino desde los rangos de GitHub: miles de
   > CIDRs rotativos publicados en `api.github.com/meta`, que no entran en un
   > Security Group (admite ~60 reglas). Restringirlo a tu IP haría que el job de
   > deploy corte por timeout.
   >
   > Lo que hace aceptable la exposición es que **solo se entra con clave**: las
   > AMIs de AWS vienen con la autenticación por contraseña deshabilitada.
   > Confirmalo en la instancia con:
   >
   > ```bash
   > sudo sshd -T | grep -i passwordauthentication   # debe decir "no"
   > ```
3. Asociar una **Elastic IP** → ese valor es el secret `EC2_HOST`. La EIP
   sobrevive al stop/start que hace el lab entre sesiones.

   > **Costo.** Desde febrero de 2024 AWS cobra **~$0.005/h (~$3.60/mes) por toda
   > IPv4 pública**, esté asociada o no. O sea que la EIP **sigue consumiendo
   > crédito con la instancia detenida** entre sesiones del lab. Son centavos por
   > día, pero en un cuatrimestre son ~$14. Si el proyecto va a quedar semanas sin
   > uso, liberala (`EC2 → Elastic IPs → Release`) y pedí otra al retomar.

### Elegir el tipo de instancia

El Learner Lab limita los tipos disponibles. Con la lista habilitada en esta
cuenta:

| Tipo | vCPU | RAM | $/hora | Veredicto |
| --- | --- | --- | --- | --- |
| `t3.micro` | 2 (burst) | 1 GB | $0.0104 | ❌ Los pods no llegan a agendarse |
| `t3.small` | 2 (burst) | 2 GB | $0.0208 | ⚠️ Al límite, requiere retocar los manifiestos |
| **`c7i-flex.large`** | **2** | **4 GB** | **~$0.085** | ✅ **Usá este** |

**Por qué `t3.micro` no es una opción.** Antes de correr un pod tuyo, el sistema
operativo se lleva ~150 MB y k3s (apiserver, etcd, scheduler, controller,
kubelet, containerd) otros ~500-600 MB; sumando CoreDNS, Traefik, local-path y
metrics-server, quedan ~850 MB de 1 GB ocupados. Pero el problema es más duro que
"anda lento": los manifiestos declaran `requests: memory: 256Mi` por servicio, y
Kubernetes suma los *requests* antes de agendar. Son 768 Mi solo del backend
contra ~700-800 MB de *allocatable* del nodo, así que **algunos pods quedarían en
`Pending` para siempre** con un evento `Insufficient memory`.

**Por qué `c7i-flex.large` y no `t3.small`.** Además de duplicar la RAM, mejora
el CPU, que es el otro cuello de botella: los `t3` son burstables con baseline de
**0.2 vCPU**, y tres Spring Boot arrancando a la vez agotan los créditos de CPU y
quedan throttleados — el arranque en frío se estira tanto que el `startupProbe`
mata los pods en loop. `c7i-flex` sostiene el 40% de dos núcleos Sapphire Rapids
(bastante más rápidos por ciclo) y puede burstear por encima. Para esta carga
—ociosa casi todo el tiempo, con picos en cada deploy— encaja bien.

> **Ojo con dejarla prendida.** `c7i-flex.large` cuesta ~4x lo que `t3.small`:
> a $0.085/h son ~$61/mes corriendo 24/7, que contra un crédito de $50-100 es
> mucho. En el uso real del lab (~12 h/semana, y el lab apaga la instancia al
> cerrar sesión) son **~$1/semana**. Solo asegurate de que quede detenida cuando
> no la usás.

> **Arquitectura.** `c7i-flex` es Intel (x86_64), que es justo lo que necesita
> este pipeline: las imágenes se construyen en runners `ubuntu-latest` de GitHub
> (amd64) sin build multi-arquitectura. Si en tu lab aparecieran tipos Graviton
> (`c7g`, `t4g`, ARM), **no sirven** tal cual: habría que agregar `platforms` a
> los `docker/build-push-action` de los dos `cd.yml`.

**Si solo tuvieras `t3.small`**, el despliegue es posible pero ajustado (~1.6 GB
allocatable contra 800 Mi de requests) y necesitarías tres cambios: bajar
`limits` a ~450Mi y `requests` a 200Mi en los tres servicios, fijar el heap
explícito (`-Xmx256m -XX:MaxMetaspaceSize=128m`) en vez de `MaxRAMPercentage`, y
arrancar k3s con `--disable metrics-server`. Avisame si hace falta y lo preparo.

### Red y VPC

**Usá la VPC default.** Ya viene con todo el cableado que este setup necesita:
un Internet Gateway adjunto, una subred pública por AZ, la route table con
`0.0.0.0/0 → igw` y *Auto-assign public IP* habilitado. No hay nada que armar.

**No armes una VPC propia.** Esto es una sola EC2 con IP pública: no hay
segmentación multi-capa que modelar, así que no te compra nada y suma formas de
fallar. Si te olvidás del Internet Gateway o de la ruta `0.0.0.0/0`, la instancia
queda sin salida y **el user data falla en silencio** — no puede descargar k3s ni
el AWS CLI. El síntoma es confuso: instancia corriendo, pero vacía.

> **La trampa cara.** Si la VPC propia usa subredes **privadas**, la instancia
> necesita un **NAT Gateway** para llegar a ECR: **~$0.045/h ≈ $32/mes** más el
> cargo por datos procesados. Es más caro que la propia instancia y te come el
> crédito del lab más rápido que cualquier otra decisión de esta arquitectura.
> Las subredes públicas de la VPC default lo evitan por completo.

Si tu cuenta **no tiene** VPC default (algunas la tienen borrada), se recrea con
todo su cableado de una sola vez:

```bash
aws ec2 create-default-vpc --region us-east-1
```

**Qué verificar** antes de seguir — que la subred asigne IP pública y que salga
por un Internet Gateway:

```bash
aws ec2 describe-subnets --subnet-ids <subnet-id> \
  --query 'Subnets[0].MapPublicIpOnLaunch'          # debe decir true

aws ec2 describe-route-tables \
  --filters Name=association.subnet-id,Values=<subnet-id> \
  --query 'RouteTables[0].Routes[?DestinationCidrBlock==`0.0.0.0/0`]'
```

### Clave SSH del runner

Acá entran en juego **dos pares de claves distintos**. No los confundas:

| | De dónde sale | Para qué sirve |
| --- | --- | --- |
| **Key pair de AWS** | AWS te lo genera al lanzar la instancia y bajás un `.pem` | Que **vos** entres por SSH |
| **Clave del runner** | **La creás vos** con `ssh-keygen` (no existe hasta que la generes) | Que **GitHub Actions** entre a desplegar |

La que va al secret `EC2_SSH_KEY` es la **privada del par del runner**.

#### Opción A (recomendada) — un par dedicado al pipeline

```powershell
ssh-keygen -t ed25519 -f runner-deploy -N '""' -C "github-actions"
```

> En PowerShell, `-N ""` **no** pasa una cadena vacía: `ssh-keygen` te va a pedir
> la passphrase de forma interactiva. Usá `-N '""'` (comillas simples por fuera).
> En Git Bash o Linux, `-N ""` funciona normal.

Eso deja dos archivos en el directorio actual:

| Archivo | Qué es | A dónde va |
| --- | --- | --- |
| `runner-deploy.pub` | pública | una línea nueva en `/home/ec2-user/.ssh/authorized_keys` de la instancia |
| `runner-deploy` | **privada** (sin extensión) | el secret `EC2_SSH_KEY` de los dos repos |

**Instalar la pública** en la instancia. Sirve igual desde tu sesión SSH o desde
Session Manager — por eso las rutas van **explícitas y con `sudo`**:

```bash
sudo mkdir -p /home/ec2-user/.ssh
echo '<contenido de runner-deploy.pub>' | sudo tee -a /home/ec2-user/.ssh/authorized_keys
sudo chown -R ec2-user:ec2-user /home/ec2-user/.ssh
sudo chmod 700 /home/ec2-user/.ssh
sudo chmod 600 /home/ec2-user/.ssh/authorized_keys
```

> **No uses `~/.ssh/authorized_keys`.** En Session Manager la sesión corre como
> `ssm-user`, así que `~` es `/home/ssm-user` y la clave terminaría en el usuario
> equivocado: el runner entra como `ec2-user` y seguiría recibiendo
> `Permission denied (publickey)`, sin ninguna pista de por qué.
>
> Los `chmod` tampoco son decorativos: `sshd` **ignora en silencio** un
> `authorized_keys` con permisos más abiertos que 600, o un `.ssh` que no sea 700
> y del propio usuario.

**Cargar la privada** como secret. Hacelo con `gh` redirigiendo el archivo: la
clave es multilínea y copiarla a mano es la fuente de error más común.

```powershell
gh secret set EC2_SSH_KEY --repo M4zls/pedidos360-backend  < runner-deploy
gh secret set EC2_SSH_KEY --repo M4zls/pedidos360-frontend < runner-deploy
```

Si la pegás por la web, tiene que ir el archivo **completo**, incluidas las
líneas `-----BEGIN OPENSSH PRIVATE KEY-----` y `-----END OPENSSH PRIVATE KEY-----`.

Ventaja de este camino: si la clave se filtra, la revocás borrando esa línea de
`authorized_keys` sin perder tu propio acceso.

#### Opción B — reusar el `.pem` de AWS

Cargás como `EC2_SSH_KEY` el mismo `.pem` que bajaste al crear la instancia y no
generás nada. Cero setup extra, pero con dos costos que conviene tener presentes:

- No podés revocarle el acceso al pipeline sin perder también el tuyo.
- La privada de tu key pair personal queda copiada en GitHub.

Para un trabajo de cursada es aceptable; para cualquier otra cosa, usá la A.

#### Verificar antes del primer deploy

Probá la clave a mano. Si esto falla, el pipeline también va a fallar, y acá el
error se lee mucho mejor que en los logs de Actions:

```bash
ssh -i runner-deploy ec2-user@<Elastic IP> 'kubectl get nodes'
```

Tiene que devolver el nodo en estado `Ready`.

> Si usás una AMI de Ubuntu el usuario es `ubuntu`, no `ec2-user`: cambialo en
> el campo `username` de los dos workflows (`cd.yml`).

## Paso 2 — Repositorios de ECR

Una sola vez, desde tu máquina con las credenciales del lab activas:

```bash
./deploy/scripts/create-ecr-repos.sh us-east-1
```

Crea `pedidos360/auth`, `pedidos360/inventory`, `pedidos360/orders` y
`pedidos360/front`, con una lifecycle policy que conserva las últimas 10
imágenes. Al terminar imprime el `ECR_REGISTRY` que va como secret.

## Paso 3 — Secrets de GitHub

En **los dos** repos (`pedidos360-backend` y `pedidos360-frontend`), en
*Settings → Secrets and variables → Actions*:

| Nombre | Tipo | Valor | Vence |
| --- | --- | --- | --- |
| `AWS_ACCESS_KEY_ID` | secret | del bloque del lab | **~4 h** |
| `AWS_SECRET_ACCESS_KEY` | secret | del bloque del lab | **~4 h** |
| `AWS_SESSION_TOKEN` | secret | del bloque del lab | **~4 h** |
| `EC2_HOST` | secret | la Elastic IP | no |
| `EC2_SSH_KEY` | secret | clave **privada** del runner (multilínea) | no |
| `ECR_REGISTRY` | **variable** | `<account>.dkr.ecr.us-east-1.amazonaws.com` | no |

> `ECR_REGISTRY` va en la pestaña **Variables**, no en Secrets: los workflows lo
> componen dentro de un output de step, y GitHub Actions descarta los outputs que
> contienen un secret. Tampoco es información sensible — el account ID aparece en
> cualquier URI de ECR.

Solo en el repo **backend**:

| Secret | Valor |
| --- | --- |
| `MICROSOFT_CLIENT_ID` | Application (client) ID de Azure |
| `LOCAL_JWT_SECRET` | clave de firma JWT (≥32 caracteres) |
| `CORS_ALLOWED_ORIGIN` | `http://<Elastic IP>` |

### El problema de las 4 horas, resuelto

El Learner Lab no permite usuarios IAM ni federación OIDC con GitHub, así que no
hay forma de tener credenciales permanentes: las tres `AWS_*` vencen cada sesión.
En vez de repegarlas a mano en 2 repos × 3 secrets:

1. En el lab: **AWS Details → AWS CLI → Show**, copiar todo el bloque.
2. En tu máquina:

```powershell
.\deploy\scripts\sync-lab-credentials.ps1
```

Lee el portapapeles, escribe `~/.aws/credentials` (para tu CLI local) y carga los
3 secrets + la variable `ECR_REGISTRY` en los dos repos con `gh`. Requiere AWS
CLI v2 y `gh auth login` hecho.

## Paso 4 — gitflow

Las ramas y lo que dispara cada una:

| Rama | CI (build+test) | Publica en ECR | Despliega |
| --- | --- | --- | --- |
| `feature/**`, `bugfix/**` | ✅ | — | — |
| PR → `develop` | ✅ | — | — |
| `develop` | — | ✅ `develop-<sha>` | — |
| `release/**`, `hotfix/**` | ✅ | — | — |
| PR → `main` | ✅ | — | — |
| `main` | — | ✅ `main-<sha>` + `latest` | ✅ |

Crear las ramas base si todavía no existen:

```bash
git checkout -b develop main && git push -u origin develop
```

Y en GitHub (**Settings → Branches**) protegé `main` y `develop` exigiendo que
pase el workflow *CI backend* antes de mergear.

> `develop` publica la imagen pero no despliega a propósito: con una sola
> instancia no hay entorno de staging separado. La imagen queda en ECR lista para
> promocionarse, que es justamente lo que un registry te permite.

## Verificación

```bash
IP=<tu Elastic IP>

curl -i http://$IP/                      # el SPA
curl -i http://$IP/api/inventory/products
curl -i http://$IP/api/orders
curl -i -X POST http://$IP/api/auth/login \
  -H 'Content-Type: application/json' 
  -d '{"username":"operador","password":"operador123"}'
```

Y dentro de la instancia:

```bash
kubectl -n pedidos360 get pods,svc,ingress,pvc
kubectl -n pedidos360 logs deployment/orders --tail=50
```

## Rollback

Esta es la ventaja concreta de tener un registry: no hay que reconstruir nada.

1. Buscar el tag al que volver:

```bash
aws ecr describe-images --repository-name pedidos360/auth --region us-east-1 \
  --query 'sort_by(imageDetails,&imagePushedAt)[-10:].[imageTags[0],imagePushedAt]' \
  --output table
```

2. En GitHub: **Actions → CD backend → Run workflow**, y poner ese tag (ej.
   `main-a1b2c3d`) en el campo `image_tag`. El job de build se saltea y solo se
   redespliega esa imagen. Lo mismo para el front.

Los PVC (`auth-data`, `inventory-data`, `orders-data`) no se tocan: las SQLite
sobreviven a deploys, rollbacks y al stop/start de la instancia.

## Rutina de cada sesión del Learner Lab

1. **Start Lab** y esperar el semáforo verde.
2. Arrancar la EC2 si el lab la apagó (`EC2 → Instances → Start`). k3s y el
   timer de ECR levantan solos por systemd.
3. Correr `sync-lab-credentials.ps1`.
4. Ya podés pushear a `main` o disparar el workflow.

## Troubleshooting

| Síntoma | Causa y arreglo |
| --- | --- |
| `ImagePullBackOff` / `no basic auth credentials` | Falta `LabInstanceProfile` en la instancia, o venció el token de ECR. Adjuntá el profile y corré `sudo systemctl start pedidos360-ecr-refresh.service`. |
| El workflow falla en *Login en ECR* | Los secrets `AWS_*` vencieron. Corré `sync-lab-credentials.ps1`. |
| Pods en `OOMKilled` | La instancia es muy chica. Necesitás 4 GB: usá `c7i-flex.large`. |
| Pods en `Pending` con evento `Insufficient memory` | Estás en una instancia de 1-2 GB. La suma de `requests` no entra en el nodo — ver [Elegir el tipo de instancia](#elegir-el-tipo-de-instancia). |
| `rollout status` da timeout | `kubectl -n pedidos360 describe pod <pod>` y `logs`. Arranque frío de Spring Boot lento: el `startupProbe` ya tolera 150 s. |
| El front carga pero la API da 404 | El Ingress del backend no está aplicado. Verificá `kubectl -n pedidos360 get ingress` — tienen que aparecer los dos. |
| `Pending` en un PVC | La StorageClass `local-path` de k3s no arrancó. `kubectl get sc` y `systemctl status k3s`. |
| `kubectl` dice `error loading config file "/etc/rancher/k3s/k3s.yaml": permission denied` | El `kubectl` de k3s apunta a ese archivo (root-only), **no** a `~/.kube/config`. El bootstrap deja el `export KUBECONFIG` en `.bashrc`, pero no aplica a la sesión en la que corriste el script: reconectate, o `export KUBECONFIG=$HOME/.kube/config`. No afecta al pipeline — los `deploy-k8s.sh` lo exportan ellos mismos. |
| `ssh`/`scp` da `Connection timed out` | Casi siempre estás usando la **IP privada**. Si empieza con `172.31.`, `10.` o `192.168.`, no es alcanzable desde internet — buscá la *Public IPv4 address* en la consola. Ojo que el hostname del prompt (`ip-172-31-25-176`) se arma con la privada, y Session Manager / Instance Connect funcionan sin la pública, así que es fácil confundirse. Si la IP es correcta, revisá que el `22` esté abierto a `0.0.0.0/0`. |
| El step de deploy queda colgado y corta por timeout | El secret `EC2_HOST` tiene la IP privada en vez de la Elastic IP, o el `22` no está abierto a `0.0.0.0/0`. El runner de GitHub no llega desde tu IP: revisá el Security Group. |
| `Permission denied (publickey)` | Tres causas, en orden de frecuencia: (1) la clave quedó en `/home/ssm-user/.ssh/` porque la instalaste con `~` desde Session Manager; (2) permisos — `sshd` ignora en silencio un `.ssh` que no sea `700` o un `authorized_keys` que no sea `600` del propio usuario; (3) el secret `EC2_SSH_KEY` no tiene la clave completa (faltan las líneas `BEGIN`/`END`). Verificá con `sudo cat /home/ec2-user/.ssh/authorized_keys` y probá `ssh -i runner-deploy ec2-user@<IP>` a mano. |
| La instancia arranca pero k3s no está (`kubectl` no existe, `systemctl status k3s` dice *unit not found*) | El user data se cortó. Mirá dónde con `sudo tail -40 /var/log/cloud-init-output.log`. Causa típica: sin salida a internet (subred sin ruta `0.0.0.0/0 → igw`, o sin IP pública). **No relances la instancia**: copiá el script y corré `sudo bash userdata-k3s.sh` — es idempotente. Después verificá con `kubectl get nodes`, no con `cloud-init status`, que queda en `error` por el primer arranque aunque ya esté resuelto. |

## Sobre el API Gateway

Con el Ingress de Traefik, el **AWS API Gateway ya no está en el camino**: el
navegador pega a `http://<IP>/` y `http://<IP>/api/*` contra el mismo origen, lo
que además elimina el problema de CORS.

Si tenés que conservarlo por requisito de la materia, sigue funcionando poniéndolo
delante: las integraciones HTTP proxy apuntan ahora a `http://<IP>/api/...`
(puerto 80) en vez de a los puertos 8080/8081/8083.
`deploy/cloudformation/api-gateway.yml` queda como referencia del shape de rutas.

## Notas de seguridad

- Hay dos puertos abiertos a internet: el `80` (la app) y el `22` (SSH). El `22`
  tiene que estar abierto porque el runner de GitHub conecta desde rangos que no
  se pueden allowlistear; lo que acota el riesgo es que **solo se entra con
  clave** (las AMIs de AWS traen la autenticación por contraseña deshabilitada).
  Si querés cerrarlo del todo, la alternativa es borrar la regla del `22` al
  terminar cada sesión del lab y recrearla al empezar la siguiente.
- La API de Kubernetes (`6443`) **no** está expuesta: solo escucha en la
  instancia y el kubeconfig no sale de ahí — el pipeline aplica por SSH.
- No hay TLS. Para un demo sobre IP pública es lo esperable, pero el JWT viaja en
  claro: no uses datos reales.
- Los datos son de demostración (SQLite + usuarios demo).
