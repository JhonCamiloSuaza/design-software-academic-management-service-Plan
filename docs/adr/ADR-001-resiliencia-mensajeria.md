# ADR-001: entrega confiable e idempotente de eventos académicos

- Estado: aceptada
- Fecha: 2026-10-16
- Responsable: Jhon Camilo Suaza Sánchez

## Contexto

El dominio académico debe conservar los eventos cuando RabbitMQ no está disponible, evitar efectos duplicados y permitir tratar mensajes que agotan sus reintentos. Publicar directamente desde una transacción de negocio puede dejar la base confirmada sin que el evento llegue al broker.

## Decisión

La transacción de negocio escribe el cambio y el evento en `academic_management.outbox`. Un relay toma los pendientes y publica en el exchange durable `academic-events` usando publisher confirms. Solo marca `published_at` después del `ack` del broker. Ante error registra el intento y programa reintentos con esperas de 1, 4 y 16 segundos; después envía a `academic-events.dlq`.

El consumidor inserta primero el par `(consumer_name, event_id)` en `processed_event` con `ON CONFLICT DO NOTHING`. Esa clave convierte las entregas repetidas en un solo efecto. El listener intenta cuatro veces con backoff 1x4, rechaza al agotar intentos y RabbitMQ enruta el mensaje a `academic-events.r1.dlq`. El endpoint protegido `POST /api/v1/events/dlq/reprocess-next` permite tomar un mensaje de esa cola y reenviarlo con confirmación; si falla, lo restaura en la DLQ.

## Consecuencias

La entrega es al menos una vez y el efecto observable es exactamente una vez por consumidor. La base conserva trazabilidad de pendientes, intentos y procesados. Se requiere ejecutar la migración 009 antes del servicio y vigilar el tamaño de outbox/DLQ. El reprocesamiento exige la autoridad `ACADEMIC_EVENTS_REPROCESS`.

## Alternativas descartadas

La publicación directa se descartó porque no comparte transacción con PostgreSQL. Una transacción distribuida se descartó por su costo operativo. Confiar solo en el `ack` del broker se descartó porque no evita que una redelivery repita efectos en el consumidor.
