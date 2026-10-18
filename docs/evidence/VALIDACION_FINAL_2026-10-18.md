# Evidencia final de ejecución — 2026-10-18

**Aprendiz:** Jhon Camilo Suaza Sanchez
**Dominio:** `academic-management`
**Entorno local:** contenedores personales `jhon-academic-*`

## 1. Esquema de persistencia

La consulta a PostgreSQL confirmó las tablas requeridas para el reto:

```text
outbox
processed_event
(2 rows)
```

![Tablas outbox y processed_event](capturas-finales/01-esquema-outbox-processed-event.png)

## 2. Idempotencia, DLQ y reproceso

La prueba `verify-r1.ps1` comprobó que dos mensajes con el mismo identificador
producen una sola recepción (`total = 1`). Luego forzó un fallo del consumidor:
la cola DLQ tuvo `messages=1`, y después del reproceso autorizado terminó con
`messages=0`.

![Salida de idempotencia, DLQ y reproceso](capturas-finales/02-r1-idempotencia-dlq-reproceso.png)

## 3. Caída y recuperación de RabbitMQ

La prueba `verify-broker-outage.ps1` detuvo RabbitMQ antes de registrar un
evento de outbox. Mientras el broker estuvo caído, la salida indicó:

```text
retry_count=1 | published=false | dead_lettered=false
```

Al iniciar RabbitMQ de nuevo, la publicación se completó:

```text
retry_count=3 | published=true | dead_lettered=false
```

![Salida de caída y recuperación del broker](capturas-finales/03-r1-caida-recuperacion-rabbitmq.png)

## 4. Frontend e integración de navegador

El MFE académico aprobó 84 pruebas. El Web Shell aprobó 112 pruebas unitarias y
la suite Playwright aprobó 3 pruebas E2E, incluido el montaje del remote
académico real.

![MFE: 84 pruebas aprobadas](capturas-finales/04-mfe-84-pruebas-aprobadas.png)

![Web Shell: 112 pruebas aprobadas](capturas-finales/05-web-shell-112-pruebas-aprobadas.png)

![Web Shell E2E: 3 pruebas aprobadas](capturas-finales/06-web-shell-e2e-3-pruebas-aprobadas.png)

