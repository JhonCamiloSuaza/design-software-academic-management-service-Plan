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
    public Queue academicEventsConsumerQueue(
            @Value("${academic.events.consumer.queue:academic-events.r1.consumer}") String queue,
            @Value("${academic.events.consumer.dlq-routing-key:academic-events.r1.dlq}") String dlqRoutingKey) {
        return QueueBuilder.durable(queue)
                .deadLetterExchange("academic-events")
                .deadLetterRoutingKey(dlqRoutingKey)
                .build();
    }

    @Bean
    public Binding academicEventsConsumerBinding(Queue academicEventsConsumerQueue,
                                                  TopicExchange academicEventsExchange) {
        return BindingBuilder.bind(academicEventsConsumerQueue)
                .to(academicEventsExchange)
                .with("academic.#");
    }

    @Bean
    public Queue academicEventsConsumerDlq(
            @Value("${academic.events.consumer.dlq:academic-events.r1.dlq}") String dlq) {
        return QueueBuilder.durable(dlq).build();
    }

    @Bean
    public Binding academicEventsConsumerDlqBinding(
            Queue academicEventsConsumerDlq,
            TopicExchange academicEventsExchange,
            @Value("${academic.events.consumer.dlq-routing-key:academic-events.r1.dlq}") String routingKey) {
        return BindingBuilder.bind(academicEventsConsumerDlq).to(academicEventsExchange).with(routingKey);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory academicEventsListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter academicEventsMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(academicEventsMessageConverter);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(4)
                .backOffOptions(1_000, 4, 16_000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build());
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public MessageConverter academicEventsMessageConverter(ObjectMapper objectMapper) {
        ObjectMapper eventObjectMapper = objectMapper.copy();
        eventObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return new Jackson2JsonMessageConverter(eventObjectMapper);
    }
}
