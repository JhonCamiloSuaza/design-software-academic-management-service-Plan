# Informe técnico breve — R1 resiliencia y mensajería

**Aprendiz:** Jhon Camilo Suaza Sanchez  
**Dominio:** `academic-management`  
**Reto:** R1 — resiliencia/mensajería

## Implementación

| Criterio | Evidencia en el código |
| --- | --- |
| Outbox transaccional | `AcademicOutboxWriter`, `AcademicOutboxJpaEntity` y tabla `academic_management.outbox`. |
| Confirmación del broker | `AcademicOutboxRelay` espera `CorrelationData.Confirm` antes de actualizar `published_at`. |
| Backoff | Publicación y consumo usan 1 s, 4 s y 16 s. |
| Consumidor idempotente | `ProcessedEventJpaRepository.insertIfAbsent` usa la clave única `(consumer_name, event_id)`. |
| DLQ | `academic-events.dlq` para relay y `academic-events.r1.dlq` para el consumidor. |
| Reproceso | `POST /api/v1/events/dlq/reprocess-next`. |
| Caída de broker | La prueba `AcademicOutboxRelayTest.postponesTheEventWhenTheBrokerIsUnavailable` verifica que el evento sigue pendiente y aumenta el contador de reintentos. |

## Validación automatizada

Ejecutado localmente:

```powershell
mvn -s "..\maven-local-settings.xml" -q -pl academic-adapters -am test
```

Resultado: pruebas unitarias aprobadas. La advertencia de Mockito sobre
auto-adjuntar el agente no representa un fallo de pruebas.

La prueba `AcademicOutboxRelayTest.postponesTheEventWhenTheBrokerIsUnavailable`
simula RabbitMQ no disponible y comprueba que `published_at` continúa nulo, que
`retry_count` aumenta a 1 y que se programa `next_attempt_at`.

## Validación de integración ejecutada

1. El entorno local levantó PostgreSQL, RabbitMQ y `academic-api`. La consulta
   `GET http://localhost:8003/actuator/health` respondió `UP`.
2. `scripts/verify-r1.ps1 -ProjectName jhon-academic` se ejecutó correctamente:
   una entrega duplicada produjo una sola fila en `processed_event`; un fallo
   forzado alcanzó `academic-events.r1.dlq` con `messages=1`; el endpoint de
   reproceso devolvió el mensaje a la ruta primaria y la DLQ terminó en
   `messages=0`.
3. `scripts/verify-broker-outage.ps1 -ProjectName jhon-academic` realiza la
   demostración controlada de caída real del broker: inserta un evento de
   práctica, detiene RabbitMQ, comprueba `retry_count >= 1` y
   `published_at = false`, lo inicia de nuevo y confirma `published_at = true`.

## Capturas para la sustentación

Durante la ejecución final captura: estado `UP`; resultado `count = 1` del
evento duplicado; cola `academic-events.r1.dlq` primero con `messages=1` y
después con `messages=0`; y las dos líneas `outbox while broker is down` y
`outbox after broker recovery`. Esas capturas corresponden a salidas reales del
entorno local del aprendiz.

