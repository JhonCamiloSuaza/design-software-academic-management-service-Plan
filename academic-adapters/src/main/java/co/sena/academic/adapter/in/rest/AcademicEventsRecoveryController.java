package co.sena.academic.adapter.in.rest;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/events")
public class AcademicEventsRecoveryController {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String dlq;

    public AcademicEventsRecoveryController(
            RabbitTemplate rabbitTemplate,
            @Value("${academic.events.exchange:academic-events}") String exchange,
            @Value("${academic.events.consumer.dlq:academic-events.r1.dlq}") String dlq) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.dlq = dlq;
    }

    @PostMapping("/dlq/reprocess-next")
    @PreAuthorize("hasAuthority('ACADEMIC_EVENTS_REPROCESS')")
    public ResponseEntity<Void> reprocessNext() {
        Message message = rabbitTemplate.receive(dlq, 1_000);
        if (message == null) {
            return ResponseEntity.notFound().build();
        }

        String routingKey = String.valueOf(
                message.getMessageProperties().getHeaders().getOrDefault("event_type", "academic.reprocessed"));
        CorrelationData correlation = new CorrelationData(message.getMessageProperties().getMessageId());
        try {
            rabbitTemplate.send(exchange, routingKey, message, correlation);
            CorrelationData.Confirm confirm = correlation.getFuture().get(10, TimeUnit.SECONDS);
            if (!confirm.isAck() || correlation.getReturned() != null) {
                throw new IllegalStateException("Broker did not confirm reprocessed event");
            }
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            rabbitTemplate.send(exchange, dlq, message);
            throw new IllegalStateException("Could not reprocess DLQ event", ex);
        }
    }
}
