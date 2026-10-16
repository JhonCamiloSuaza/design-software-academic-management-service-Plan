package co.sena.academic.adapter.in.event;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AcademicEventConsumerTest {

    @Test
    void duplicateDeliveryUsesAtomicInsertAndDoesNotCreateASecondEffect() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any())).thenReturn(1, 0);
        AcademicEventConsumer consumer = new AcademicEventConsumer(jdbcTemplate);

        MessageProperties properties = new MessageProperties();
        properties.setMessageId(UUID.randomUUID().toString());
        properties.setHeader("event_type", "academic.ficha.opened");
        Message message = new Message("{\"ficha_id\":\"f-1\"}".getBytes(StandardCharsets.UTF_8), properties);

        consumer.consume(message);
        consumer.consume(message);

        verify(jdbcTemplate, times(2)).update(anyString(), any(), any(), any(), any());
    }
}
