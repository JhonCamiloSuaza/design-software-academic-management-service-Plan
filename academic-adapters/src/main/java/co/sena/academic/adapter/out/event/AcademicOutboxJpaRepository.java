package co.sena.academic.adapter.out.event;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AcademicOutboxJpaRepository extends JpaRepository<AcademicOutboxJpaEntity, UUID> {

    @Query("""
            select event from AcademicOutboxJpaEntity event
            where event.publishedAt is null
              and event.deadLetteredAt is null
              and event.nextAttemptAt <= :now
            order by event.createdAt asc
            """)
    List<AcademicOutboxJpaEntity> findPublishable(Instant now, Pageable pageable);
}
