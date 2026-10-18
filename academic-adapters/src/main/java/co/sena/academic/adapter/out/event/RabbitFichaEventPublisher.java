package co.sena.academic.adapter.out.event;

import co.sena.academic.application.port.out.FichaEventPublisherPort;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/** Production RabbitMQ adapter for academic.ficha.opened. */
public class RabbitFichaEventPublisher implements FichaEventPublisherPort {

    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange academicEventsExchange;
    private final String routingKey;

    public RabbitFichaEventPublisher(RabbitTemplate rabbitTemplate,
                                     TopicExchange academicEventsExchange,
                                     String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.academicEventsExchange = academicEventsExchange;
        this.routingKey = routingKey;
    }

    @Override
    public void publish(EventEnvelope event) {
        rabbitTemplate.convertAndSend(
                academicEventsExchange.getName(),
                routingKey,
                event,
                message -> {
                    MessageProperties properties = message.getMessageProperties();
                    properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                    properties.setMessageId(event.event_id().toString());
                    properties.setHeader("event_type", event.event_type());
                    properties.setHeader("event_version", event.version());
                    return message;
                });
    }
}
