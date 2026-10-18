package co.sena.academic.adapter.out.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventJpaEntity, UUID> {

    /** PostgreSQL atomically accepts only the first delivery of an event. */
    @Modifying
    @Query(value = """
            insert into academic_management.processed_event
                (id, event_id, event_type, consumer_name, processed_at)
            values (gen_random_uuid(), :eventId, :eventType, :consumerName, now())
            on conflict (consumer_name, event_id) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("eventId") UUID eventId,
                       @Param("eventType") String eventType,
                       @Param("consumerName") String consumerName);
}
