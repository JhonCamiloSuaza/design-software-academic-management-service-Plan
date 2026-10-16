package co.sena.academic.adapter.out.event;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AcademicOutboxRelayTest {

    @Test
    void marksPublishedOnlyAfterBrokerAck() {
        AcademicOutboxJpaRepository repository = mock(AcademicOutboxJpaRepository.class);
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        AcademicOutboxJpaEntity event = event();
        when(repository.findPublishable(any(Instant.class), any(Pageable.class))).thenReturn(List.of(event));
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).send(anyString(), anyString(), any(), any(CorrelationData.class));

        new AcademicOutboxRelay(repository, rabbitTemplate, 10).publishPending();

        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getRetryCount()).isZero();
    }

    @Test
    void schedulesRetryWhenBrokerNacks() {
        AcademicOutboxJpaRepository repository = mock(AcademicOutboxJpaRepository.class);
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        AcademicOutboxJpaEntity event = event();
        when(repository.findPublishable(any(Instant.class), any(Pageable.class))).thenReturn(List.of(event));
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(false, "broker unavailable"));
            return null;
        }).when(rabbitTemplate).send(anyString(), anyString(), any(), any(CorrelationData.class));

        new AcademicOutboxRelay(repository, rabbitTemplate, 10).publishPending();

        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getLastError()).contains("publisher confirm failed");
    }

    private AcademicOutboxJpaEntity event() {
        Instant now = Instant.now();
        return new AcademicOutboxJpaEntity(
                UUID.randomUUID(), UUID.randomUUID(), "academic.ficha.opened",
                "academic-events", "academic.ficha.opened", "{}", now);
    }
}
