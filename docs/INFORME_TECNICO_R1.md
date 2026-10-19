# Informe técnico breve — R1 resiliencia y mensajería

**Aprendiz:** Jhon Camilo Suaza Sanchez
**Dominio:** `academic-management`
**Reto:** R1 — resiliencia/mensajería
**Fecha de validación final:** 18 de octubre de 2026

## Alcance

Se dejó operativo el recorrido local del dominio académico: PostgreSQL, RabbitMQ,
Liquibase, API `academic-management`, MFE académico y Web Shell. Las pruebas se
ejecutaron desde las ramas `develop` personales después de fusionar los PR del reto.

## Implementación

| Criterio | Implementación |
| --- | --- |
| Outbox transaccional | `AcademicOutboxWriter`, `AcademicOutboxJpaEntity` y tabla `academic_management.outbox`. |
| Confirmación del broker | `AcademicOutboxRelay` espera la confirmación de RabbitMQ antes de registrar `published_at`. |
| Reintentos con backoff | Reintentos de 1 s, 4 s y 16 s para la mensajería R1. |
| Consumidor idempotente | `processed_event` tiene unicidad por `(consumer_name, event_id)`. |
| DLQ y reproceso | Cola `academic-events.r1.dlq` y endpoint `POST /api/v1/events/dlq/reprocess-next`. |
| Recuperación del broker | El evento permanece en outbox durante la caída y se publica al recuperar RabbitMQ. |

La decisión de arquitectura está documentada en
[ADR-001 — R1 resiliencia y mensajería](adr/ADR-001-r1-resiliencia-mensajeria.md).

## Validación final

| Prueba | Resultado observado |
| --- | --- |
| Salud de API | `GET /actuator/health` respondió `UP`. |
| Esquema | Existen las tablas `outbox` y `processed_event` en `academic_management`. |
| Idempotencia | Dos publicaciones del mismo evento dejaron una sola recepción: `total = 1`. |
| DLQ | Un fallo controlado terminó con `academic-events.r1.dlq | messages=1 | ready=1`. |
| Reproceso | Después del reproceso autorizado, la DLQ terminó en `messages=0 | ready=0`. |
| Caída de RabbitMQ | Durante la caída: `retry_count=1`, `published=false`; al recuperarlo: `retry_count=3`, `published=true`. |
| Caos con reinicio de API | Con RabbitMQ detenido, el evento `9baa5104-2b4c-4fb6-bc97-d913c58723ef` permaneció pendiente (`1|f|f`). La API se reinició antes de recuperar el broker. Después, el evento quedó publicado (`2|t|f`) y `processed_event=1`; una segunda entrega mantuvo el total en 1. Fue un evento de práctica insertado directamente en outbox. |
| Creación de ficha y outbox | `POST /api/v1/fichas` respondió `201` para `R1-361bac39`; PostgreSQL confirmó la ficha y su evento `academic.ficha.opened` con el mismo `ficha_id`; `GET` respondió `200` y el Shell mostró su detalle. La dependencia de centros y el token fueron de práctica. |
| Backend | `mvn -s "..\\maven-local-settings.xml" -q -pl academic-adapters -am test` finalizó correctamente. |
| MFE académico | `npm.cmd test`: 84 pruebas aprobadas. |
| Web Shell | `npm.cmd test`: 112 pruebas aprobadas. |
| Integración de navegador | `npm.cmd run test:e2e`: 3 pruebas Playwright aprobadas. |

Para ejecutar los scripts de evidencia en este equipo se usó
`powershell -ExecutionPolicy Bypass -File ...`. Es un permiso temporal del
proceso de PowerShell; no cambia la política permanente del equipo ni el código.

## Evidencias

Las salidas y capturas de la ejecución final están en
[VALIDACION_FINAL_2026-10-18.md](evidence/VALIDACION_FINAL_2026-10-18.md).

- [Esquema outbox y processed_event](evidence/capturas-finales/01-esquema-outbox-processed-event.png)
- [Idempotencia, DLQ y reproceso](evidence/capturas-finales/02-r1-idempotencia-dlq-reproceso.png)
- [Caída y recuperación de RabbitMQ](evidence/capturas-finales/03-r1-caida-recuperacion-rabbitmq.png)
- [MFE: 84 pruebas aprobadas](evidence/capturas-finales/04-mfe-84-pruebas-aprobadas.png)
- [Web Shell: 112 pruebas aprobadas](evidence/capturas-finales/05-web-shell-112-pruebas-aprobadas.png)
- [Web Shell E2E: 3 pruebas aprobadas](evidence/capturas-finales/06-web-shell-e2e-3-pruebas-aprobadas.png)
- [RabbitMQ: DLQ R1 vacía](evidence/capturas-finales/07-rabbitmq-dlq-vacia.png)
- [Ficha creada por API y visible en el Shell](evidence/capturas-finales/08-ficha-creada-outbox-negocio.png)
- [Salida de caos con reinicio de API y duplicado](evidence/VALIDACION_FINAL_2026-10-18.md#7-caos-con-reinicio-de-la-api-y-entrega-duplicada)

## Conclusión

Se validó el recorrido local presentado para R1, incluida la escritura de una
ficha mediante la API y su evento en outbox. La prueba adicional confirma el
camino exitoso; no forzó un rollback ni sustituyó el servicio de centros o el IAM
de práctica por sus implementaciones reales. No se realizaron cambios en
repositorios de la organización `code-sena`.
