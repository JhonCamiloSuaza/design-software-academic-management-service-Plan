# ADR-001 — Mensajería resiliente para academic-management (R1)

- **Estado:** aceptada
- **Responsable:** Jhon Camilo Suaza Sanchez
- **Fecha:** 2026-10-17

## Contexto

El dominio `academic-management` publica eventos cuando cambia información de
programas, competencias o fichas. Una caída de RabbitMQ no puede hacer que se
pierda el cambio ya confirmado en PostgreSQL. RabbitMQ también puede entregar
un mensaje más de una vez, por lo que el consumidor debe ser idempotente.

## Decisión

1. La transacción de negocio escribe el evento en `academic_management.outbox`.
   El relay lo publica después y solo marca `published_at` cuando recibe el
   *publisher confirm* positivo del broker.
2. Ante una publicación fallida, el relay conserva el registro y programa los
   intentos con backoff de **1 s, 4 s y 16 s**. Tras agotarlos, lo envía a
   `academic-events.dlq`.
3. El consumidor R1 escucha `academic-events.r1.consumer`. Su contenedor vuelve
   a intentar un fallo con la misma política y después RabbitMQ lo envía a
   `academic-events.r1.dlq` mediante el exchange `academic-events.r1.dlx`.
4. `academic_management.processed_event` tiene una restricción única por
   `(consumer_name, event_id)`. Un `INSERT ... ON CONFLICT DO NOTHING` decide
   atómicamente si la entrega es la primera; las repetidas no producen otro
   efecto.
5. Un operador autorizado puede ejecutar `POST /api/v1/events/dlq/reprocess-next`.
   Se conserva el `message_id` para la trazabilidad e idempotencia.

## Consecuencias

- Un mensaje no se considera publicado solo porque se llamó a RabbitTemplate;
  requiere confirmación del broker.
- Los eventos en DLQ requieren una intervención explícita y quedan visibles en
  la consola de RabbitMQ.
- La tabla `processed_event` crece con los eventos consumidos; se debe definir
  retención operativa si el volumen real lo exige.

## Alternativas descartadas

- Publicar directamente desde el caso de uso: puede perder eventos si la base
  confirma y RabbitMQ cae.
- Confiar en el ACK de RabbitMQ sin una clave de deduplicación: permite efectos
  duplicados en redeliveries.
