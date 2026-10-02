package com.manas.regfiling.event;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The only write path to the filing_event table. {@code append} always calls
 * {@code EntityManager.persist}, which Hibernate always translates to a SQL INSERT for a new
 * entity with an identity generator, never an UPDATE or a MERGE statement. There is no update or
 * delete method anywhere in this class, and {@link FilingEventReadRepository} cannot provide one
 * either (it extends the bare marker interface, not JpaRepository). The invariant this is meant
 * to hold is exercised directly in AppendOnlyEventLogInvariantTest, which installs a Hibernate
 * statement inspector and asserts no UPDATE or DELETE statement is ever issued against this
 * table across a full filing lifecycle.
 */
@Component
public class FilingEventStore {

    private final EntityManager entityManager;
    private final FilingEventReadRepository readRepository;

    public FilingEventStore(EntityManager entityManager, FilingEventReadRepository readRepository) {
        this.entityManager = entityManager;
        this.readRepository = readRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public FilingEvent append(FilingEvent event) {
        entityManager.persist(event);
        return event;
    }

    public int nextSequence(Long filingId) {
        return (int) readRepository.countByFilingId(filingId);
    }

    public List<FilingEvent> findByFilingId(Long filingId) {
        return readRepository.findByFilingIdOrderBySequenceAsc(filingId);
    }
}
