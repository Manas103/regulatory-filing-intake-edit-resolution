package com.manas.regfiling.event;

import org.springframework.data.repository.Repository;
import java.util.List;

/**
 * Deliberately extends the bare Spring Data {@link Repository} marker interface, not
 * {@code JpaRepository}. That marker exposes no save, delete or update methods at all, only the
 * derived query methods declared below. There is structurally no way to mutate or remove a row
 * through this interface; see {@link FilingEventStore#append} for the only way rows enter this
 * table, which uses {@code EntityManager.persist}, never {@code merge}.
 */
public interface FilingEventReadRepository extends Repository<FilingEvent, Long> {

    List<FilingEvent> findByFilingIdOrderBySequenceAsc(Long filingId);

    long countByFilingId(Long filingId);
}
