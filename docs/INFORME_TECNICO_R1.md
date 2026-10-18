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

## Validación de integración que se debe capturar

1. Levantar el entorno con `docker-infra/scripts/up-academic-stack.ps1`.
2. Ejecutar `scripts/verify-r1.ps1` en este repositorio.
3. Capturar: estado saludable, una fila de `processed_event`, `count = 1` para
   el evento duplicado, una cola DLQ con mensaje y su valor `0` después del
   reproceso.

Las salidas de ese procedimiento se adjuntarán como evidencia final, porque se
generan sobre el entorno local del aprendiz y no deben inventarse en este
informe.
