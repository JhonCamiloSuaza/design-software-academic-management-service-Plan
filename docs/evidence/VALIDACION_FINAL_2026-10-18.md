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


## 5. Evidencia visual de RabbitMQ

La consola de administración de RabbitMQ muestra la cola
`academic-events.r1.dlq` en ejecución, con `Ready=0`, `Unacked=0` y `Total=0`
después del reproceso. También se observa la cola del consumidor R1.

![RabbitMQ: DLQ R1 vacía](capturas-finales/07-rabbitmq-dlq-vacia.png)

## 6. Operación de negocio real y outbox

Se ejecutó una prueba adicional de creación de ficha mediante la API académica,
con el guion reproducible [`verify-business-outbox.ps1`](../../scripts/verify-business-outbox.ps1):

```powershell
powershell -ExecutionPolicy Bypass -File ".\scripts\verify-business-outbox.ps1" -ProjectName jhon-academic
```

El guion envió `POST /api/v1/fichas` con programa y centro de práctica, fecha
futura, jornada diurna, modalidad presencial y cupo 30. La API respondió `201`:

```text
HTTP 201 | ficha_number=R1-361bac39 | ficha_id=ceb35e9d-2242-4176-a5cd-3a36988d5d51
enrollment_ficha: ceb35e9d-2242-4176-a5cd-3a36988d5d51|R1-361bac39
outbox: 4412e478-cbe1-4873-8039-6c19a8954be2|academic.ficha.opened|t|t
```

En la última fila, el primer `t` confirma que el contenido del evento referencia
el mismo ID de ficha; el segundo indica que el relay ya lo había publicado. Una
consulta SQL independiente volvió a unir la ficha y el evento por ese ID. El
`GET /api/v1/fichas/ceb35e9d-2242-4176-a5cd-3a36988d5d51` respondió `200`.
Al actualizar el Web Shell, la nueva ficha apareció en una lista de dos registros
y se abrió su detalle:

![Ficha R1 creada mediante la API y visible en el Shell](capturas-finales/08-ficha-creada-outbox-negocio.png)

El guion usa la API académica, PostgreSQL y RabbitMQ reales del entorno local.
Como el servicio de centros de formación no estaba levantado, inicia durante el
`POST` una respuesta temporal para el centro de práctica y la detiene al final;
la autenticación también usa el perfil local `dev`. La prueba confirma el camino
exitoso de ficha y outbox, pero no fuerza un rollback ni valida IAM o referencia
de centros reales. La prueba de caída del broker de la sección 3 cubre por
separado la conservación y publicación posterior de un evento pendiente.
