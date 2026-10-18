# Evidencia local de validación — 2026-10-18

**Aprendiz:** Jhon Camilo Suaza Sanchez  
**Dominio:** `academic-management`  
**Entorno:** contenedores personales `jhon-academic-*`

## Servicios disponibles

| Componente | Resultado |
| --- | --- |
| `academic-api` | `GET /actuator/health` respondió `UP`. |
| PostgreSQL | Contenedor saludable y esquema `academic_management` migrado. |
| RabbitMQ | Contenedor saludable; DLQ R1 final en `messages=0`, `ready=0`, `unacknowledged=0`. |

La consulta funcional `GET /api/v1/fichas?page=1&page_size=10`, con el token
de práctica local, respondió **HTTP 200** y devolvió una ficha de prueba
(`2874521`). Esto confirma el recorrido API y PostgreSQL que consume el MFE.

## Entrega duplicada e idempotencia

`verify-r1.ps1` publicó dos veces el mismo `message_id`. La consulta a
`academic_management.processed_event` devolvió exactamente una fila para ese
evento:

```text
event_type                       | total
academic.r1.duplicate.practice   | 1
```

## Reintentos, DLQ y reprocesamiento

La misma validación envió un evento con `force_failure=true`. RabbitMQ lo
movó a `academic-events.r1.dlq` después de los reintentos de 1 s, 4 s y 16 s:

```text
academic-events.r1.dlq | messages=1 | ready=1
```

El endpoint autorizado `POST /api/v1/events/dlq/reprocess-next` lo repuso en la
ruta primaria. El estado final comprobado fue:

```text
academic-events.r1.dlq | messages=0 | ready=0 | unacknowledged=0
```

## Caída y recuperación del broker

`verify-broker-outage.ps1` detuvo RabbitMQ antes de crear el evento de prueba.
Mientras el broker estuvo detenido, la fila del outbox fue:

```text
375f206b-054a-4bc5-bba1-d12861a5ec8a | retry_count=1 | published=false | dead_lettered=false
```

Al iniciar de nuevo RabbitMQ, el relay conservó el evento y terminó su
publicación:

```text
375f206b-054a-4bc5-bba1-d12861a5ec8a | retry_count=3 | published=true | dead_lettered=false
```

## Shell y microfrontend académico

La captura [academic-shell-2026-10-18.png](academic-shell-2026-10-18.png) se
generó en Chromium contra `http://localhost:5000/academic-management/`, con el
Shell federando el remote real en `http://localhost:5103/remoteEntry.js`.
La comprobación automatizada devolvió `heading=Fichas`, `mfe=1` e
`invalid-hook=false`. El smoke test Playwright del Shell también finalizó con
`3 passed`.
