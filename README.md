# design-software-academic-management-service (Java/Spring Boot)

Esqueleto hexagonal Maven multi-modulo (ADR-006): core (dominio+aplicacion puros) / adapters / apps.
Modulos: academic-core academic-adapters academic-management-api. Build: ./mvnw package (requiere Maven).

## HU-ACAD-11/12/13

- `PATCH /api/v1/fichas/{id}/status` aplica la maquina de estados de ficha.
- `DELETE /api/v1/fichas/{id}` es un alias semantico de cancelacion: cambia la ficha a `CANCELLED`; no existe borrado fisico ni columna `deleted_at`.
- La cancelacion requiere `ACADEMIC_FICHA_STATUS` y rol `CENTER_DIRECTOR`.
- Eventos publicados en `academic-events` mediante outbox transaccional: `academic.program.created`, `academic.ficha.opened`, `academic.ficha.closed` y `academic.competency.updated`.
- El relay de outbox usa RabbitMQ/AMQP, reintentos `1s`, `4s`, `16s` y DLQ `academic-events.dlq` con retencion de 7 dias.
