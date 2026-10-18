package co.sena.academic.adapter.out.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
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
    public DirectExchange academicR1DeadLetterExchange() {
        return ExchangeBuilder.directExchange("academic-events.r1.dlx").durable(true).build();
    }

    @Bean
    public Queue academicR1ConsumerQueue() {
        return QueueBuilder.durable("academic-events.r1.consumer")
                .deadLetterExchange("academic-events.r1.dlx")
                .deadLetterRoutingKey("academic-events.r1.dlq")
                .build();
    }

    @Bean
    public Queue academicR1Dlq() {
        return QueueBuilder.durable("academic-events.r1.dlq").build();
    }

    @Bean
    public Binding academicR1ConsumerBinding(Queue academicR1ConsumerQueue, TopicExchange academicEventsExchange) {
        return BindingBuilder.bind(academicR1ConsumerQueue).to(academicEventsExchange).with("academic.r1.#");
    }

    @Bean
    public Binding academicR1DlqBinding(Queue academicR1Dlq, DirectExchange academicR1DeadLetterExchange) {
        return BindingBuilder.bind(academicR1Dlq).to(academicR1DeadLetterExchange).with("academic-events.r1.dlq");
    }

    @Bean
    public MessageConverter academicEventsMessageConverter(ObjectMapper objectMapper) {
        ObjectMapper eventObjectMapper = objectMapper.copy();
        eventObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return new Jackson2JsonMessageConverter(eventObjectMapper);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory academicListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter academicEventsMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(academicEventsMessageConverter);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(4)
                .backOffOptions(1_000, 4.0, 16_000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build());
        return factory;
    }
}
