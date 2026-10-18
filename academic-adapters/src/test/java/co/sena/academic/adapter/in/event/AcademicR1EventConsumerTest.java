package co.sena.academic.adapter.in.event;

import co.sena.academic.adapter.out.event.ProcessedEventJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AcademicR1EventConsumerTest {

    @Test
    void persistsTheIdempotencyReceiptFromMessageMetadata() {
        ProcessedEventJpaRepository repository = mock(ProcessedEventJpaRepository.class);
        AcademicR1EventConsumer consumer = new AcademicR1EventConsumer(repository);
        UUID eventId = UUID.randomUUID();
        MessageProperties properties = new MessageProperties();
        properties.setMessageId(eventId.toString());
        properties.setHeader("event_type", "academic.r1.duplicate.practice");
        when(repository.insertIfAbsent(eventId, "academic.r1.duplicate.practice", "academic-management-r1"))
                .thenReturn(1);

        consumer.consume(new Message("{}".getBytes(StandardCharsets.UTF_8), properties));

        verify(repository).insertIfAbsent(eventId, "academic.r1.duplicate.practice", "academic-management-r1");
    }

    @Test
    void failsBeforeRecordingWhenTheChaosFlagIsPresent() {
        ProcessedEventJpaRepository repository = mock(ProcessedEventJpaRepository.class);
        AcademicR1EventConsumer consumer = new AcademicR1EventConsumer(repository);
        MessageProperties properties = new MessageProperties();
        properties.setMessageId(UUID.randomUUID().toString());
        properties.setHeader("force_failure", true);

        assertThatThrownBy(() -> consumer.consume(new Message("{}".getBytes(StandardCharsets.UTF_8), properties)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("forced consumer failure");
    }
}
