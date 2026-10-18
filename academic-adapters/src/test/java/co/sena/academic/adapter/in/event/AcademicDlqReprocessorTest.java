package co.sena.academic.adapter.in.event;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AcademicDlqReprocessorTest {

    @Test
    void republishesOneMessageWithoutChangingItsIdentifier() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        MessageProperties properties = new MessageProperties();
        properties.setMessageId("c2fd784a-5e45-4f3d-b9be-f96e60a944c0");
        Message message = new Message("{}".getBytes(), properties);
        when(rabbitTemplate.receive("academic-events.r1.dlq")).thenReturn(message);
        AcademicDlqReprocessor reprocessor = new AcademicDlqReprocessor(
                rabbitTemplate, "academic-events.r1.dlq", "academic-events", "academic.r1.#");

        var result = reprocessor.reprocessNext();

        assertThat(result.reprocessed()).isTrue();
        assertThat(result.eventId()).isEqualTo(properties.getMessageId());
        verify(rabbitTemplate).send(eq("academic-events"), eq("academic.r1.reprocess"), any(Message.class));
    }
}
