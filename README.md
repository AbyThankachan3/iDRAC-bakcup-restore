# iDRAC Backup & Restore

A Java Spring Boot application for automating backup and restore of Dell iDRAC server configurations using the Redfish API. Supports bulk iDRAC server registration, backups and SCP (Server Configuration Profile) export/import.

---

## Project Structure

```
iDRAC-bakcup-restore/
├── astraeus/                        # Main Spring Boot microservice
├── idrac-redfish-lib/               # Reusable iDRAC Redfish client library
├── idrac-backup-restore-helm-chart/ # Helm chart for Kubernetes deployment
├── Dockerfile
└── pom.xml                          # Root multi-module POM
```

## Modules

| Module | Description |
|---|---|
| `astraeus` | Core microservice — REST APIs, backup/restore orchestration, job tracking|
| `idrac-redfish-lib` | Feign-based iDRAC Redfish API client library — SCP export/import, job polling |
| `idrac-backup-restore-helm-chart` | Helm chart for deploying the full stack to Kubernetes |

## Tech Stack

- **Java 17** + **Spring Boot**
- **PostgreSQL** — persistent storage for servers, jobs, and logs
- **HashiCorp Vault** — secure credential storage for iDRAC credentials
- **OpenFeign** — HTTP client for Redfish API communication
- **Flyway** — database schema migrations
- **Lombok** + **Jackson** — boilerplate reduction and JSON handling
- **Docker** + **Kubernetes** + **Helm** — containerisation and deployment
- **Sealed Secrets** — encrypted Kubernetes secrets safe for git

---

## Deployment

### Prerequisites

- Kubernetes cluster (or Minikube)
- Helm 3+
- `kubeseal` CLI and Sealed Secrets controller installed on the cluster

### 1. Set up secrets

Copy the example secret files and fill in your values:

```bash
cp idrac-backup-restore-helm-chart/secret-examples/postgres-secret.yaml ./postgres-secret.yaml
cp idrac-backup-restore-helm-chart/secret-examples/vault-secret.yaml ./vault-secret.yaml
```

Edit `postgres-secret.yaml` with your database credentials and `vault-secret.yaml` with your Vault root token.

### 2. Seal the secrets

```bash
kubeseal --format yaml < postgres-secret.yaml > postgres-sealed-secret.yaml
kubeseal --format yaml < vault-secret.yaml > vault-sealed-secret.yaml
```

### 3. Install the Helm chart

```bash
helm install idrac-backup-restore ./idrac-backup-restore-helm-chart
```

### 4. Apply the sealed secrets

```bash
kubectl apply -f postgres-sealed-secret.yaml
kubectl apply -f vault-sealed-secret.yaml
```

### 5. Get the service URL

Get the namespace from your `values.yaml` and run:

```bash
kubectl get svc idrac-service -n <your-namespace>
```

This will show the NodePort assigned to the service:

```
NAME            TYPE       CLUSTER-IP     EXTERNAL-IP   PORT(S)        AGE
idrac-service   NodePort   10.96.x.x      <none>        80:3xxxx/TCP   1m
```

Then access the application at `http://<node-ip>:<node-port>` where `<node-ip>` is:

| Environment | How to get the node IP |
|---|---|
| Minikube | `minikube ip` |
| Kind | `kubectl get nodes -o wide` |
| Cloud (EKS/GKE/AKS) | Use the `EXTERNAL-IP` from `kubectl get svc` |
| Bare metal | IP of any node in your cluster |

### Upgrade

```bash
helm upgrade idrac-backup-restore ./idrac-backup-restore-helm-chart
```

### Uninstall

```bash
helm uninstall idrac-backup-restore
```

---

## API Reference

Swagger UI is available at:

```
http://<node-ip>:<node-port>/swagger-ui/index.html
```

---

## Documentation

Complete documentation is available at: https://www.notion.so/iDRAC-Backup-and-Restore-Tool-3172f2e2f62a80029c34c9e23d1ba247
