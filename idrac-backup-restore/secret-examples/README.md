# Setting up secrets

1. Copy the example secret files out of this folder
2. Fill in your values
3. Seal them against your cluster:

   kubeseal --format yaml < postgres-secret.yaml > postgres-sealed-secret.yaml
   kubeseal --format yaml < vault-secret.yaml > vault-sealed-secret.yaml

4. Apply the sealed secrets to your cluster:

   kubectl apply -f postgres-sealed-secret.yaml
   kubectl apply -f vault-sealed-secret.yaml

5. Then install the chart:

   helm install idrac-backup-restore ./idrac-backup-restore
```

**`.gitignore`** — make sure raw secrets are never accidentally committed:
```
secret-examples/postgres-secret.yaml
secret-examples/vault-secret.yaml
*-sealed-secret.yaml