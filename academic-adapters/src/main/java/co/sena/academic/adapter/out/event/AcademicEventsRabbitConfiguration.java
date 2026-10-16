package co.sena.academic.adapter.out.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** RabbitMQ topology and JSON serialization for academic domain events. */
@Configuration
public class AcademicEventsRabbitConfiguration {

    @Bean
    public TopicExchange academicEventsExchange(
            @Value("${academic.events.exchange:academic-events}") String exchangeName) {
        return ExchangeBuilder.topicExchange(exchangeName)
                .durable(true)
                .build();
    }

    @Bean
    public Queue academicEventsDlq() {
        return QueueBuilder.durable("academic-events.dlq")
                .ttl(604_800_000)
                .build();
    }

    @Bean
    public Binding academicEventsDlqBinding(Queue academicEventsDlq, TopicExchange academicEventsExchange) {
        return BindingBuilder.bind(academicEventsDlq)
                .to(academicEventsExchange)
                .with("academic-events.dlq");
    }

    @Bean
    public MessageConverter academicEventsMessageConverter(ObjectMapper objectMapper) {
        ObjectMapper eventObjectMapper = objectMapper.copy();
        eventObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return new Jackson2JsonMessageConverter(eventObjectMapper);
    }
}
