# API Quick Reference

Base URL: `http://<node-ip>:<node-port>/api/v1`

Full interactive docs: `http://<node-ip>:<node-port>/swagger-ui/index.html`

---

## Server Management `/api/v1/servers`

Register and manage iDRAC servers before running backups or restores.

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/servers` | Register a single server |
| `POST` | `/servers/file` | Bulk register servers via CSV/file upload |
| `GET` | `/servers/job/{jobId}` | Check status of any registration job (single or bulk) |
| `GET` | `/servers` | List all registered servers |
| `GET` | `/servers/{host}` | Get details of a specific server by IP |
| `DELETE` | `/servers/{host}` | Remove a server from the system |

**Typical registration flow:**
```
POST /servers              → returns { jobId, status: "STARTED" }
POST /servers/file         → returns { jobId, status: "STARTED" }
GET  /servers/job/{jobId}  → poll until status is COMPLETED or FAILED
GET  /servers              → verify server appears in the list
```

> Note: Both single and bulk registration return a `jobId`. Use `GET /servers/job/{jobId}` to track the status of either.

---

## Backup `/api/v1/backup`

Trigger backups and query results. All backup jobs run asynchronously — submit the job, get back a `jobId`, and poll for status using that `jobId`.

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/backup` | Trigger backup for all registered servers |
| `POST` | `/backup/{host}` | Trigger backup for a specific server |
| `GET` | `/backup/jobs` | List all backup jobs |
| `GET` | `/backup/jobs/{id}` | Get details and per-host status of a specific job — use the `jobId` returned from POST |
| `GET` | `/backup/hosts/{host}/success` | Get all successful backups for a host (supports `?from=` and `?to=` filters) |
| `GET` | `/backup/hosts/{host}/latest` | Get the latest successful backup for a host |
| `GET` | `/backup/models/success` | Get successful backups grouped by server model |
| `GET` | `/backup/download/{logId}` | Download the backup XML file — use `logId` from the `hostLogs` list in `GET /backup/jobs/{id}` |

**Typical backup flow:**
```
POST /backup                  → returns { jobId, status: "STARTED" }
GET  /backup/jobs/{jobId}     → poll until status is COMPLETED or FAILED
                                response includes hostLogs[] with individual logId per server
GET  /backup/download/{logId} → download the backup file using logId from hostLogs
```

---

## Restore `/api/v1/restore`

Restore a server configuration from a previously taken backup. The `logId` in the POST is the `logId` from an individual host backup entry. Runs asynchronously.

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/restore/{logId}` | Start a restore using a `logId` from a completed backup |
| `GET` | `/restore/{restoreId}` | Get current status and progress — use the `restoreId` returned from POST |
| `GET` | `/restore/{restoreId}/diagnostics` | Get detailed failures and warnings after restore completes |

**Typical restore flow:**
```
GET  /backup/jobs/{jobId}              → find a completed backup job
                                         hostLogs[] contains logId per server
                                         OR
GET  /backup/hosts/{host}/latest       → get logId of latest backup for a host
POST /restore/{logId}                  → returns { restoreId, status: "STARTED" }
GET  /restore/{restoreId}              → poll until status is COMPLETED or COMPLETED_WITH_ERRORS or FAILED
GET  /restore/{restoreId}/diagnostics  → check failures and warnings
```

**Restore request body:**
```json
{
  "host": "your-new-host",
  "username": "your-username",
  "password": "your-password"
}
```

> Note: `host`, `username`, and `password` in the request body are the credentials of the **target iDRAC server** where the backup will be restored — this can be the same server or a different one.