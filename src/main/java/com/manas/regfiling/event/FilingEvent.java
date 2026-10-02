package com.manas.regfiling.event;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * One immutable row in the append only event log. Rows are never updated and never deleted,
 * see {@link FilingEventStore}: that class exposes only an insert style {@code append} method
 * and read queries, nothing that issues SQL UPDATE or DELETE against this table. The fields are
 * kept as plain nullable columns rather than a JSON blob so the replay projector in
 * {@link com.manas.regfiling.replay.FilingReplayProjector} can fold over them without a JSON
 * parser in the hot path.
 */
@Entity
@Table(name = "filing_event")
public class FilingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long filingId;

    @Column(nullable = false)
    private Integer sequence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FilingEventType eventType;

    /** Set for EDIT_FAILURE_RECORDED and EXPLANATION_ADDED, the edit_failure row this refers to. */
    @Column
    private Long editFailureId;

    @Column
    private String ruleId;

    @Enumerated(EnumType.STRING)
    @Column
    private com.manas.regfiling.domain.EditSeverity severity;

    @Column
    private String scheduleCode;

    @Column
    private String lineItemCode;

    @Column(length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column
    private com.manas.regfiling.domain.FilingStatus statusBefore;

    @Enumerated(EnumType.STRING)
    @Column
    private com.manas.regfiling.domain.FilingStatus statusAfter;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected FilingEvent() {
    }

    public static FilingEvent of(Long filingId, int sequence, FilingEventType type) {
        FilingEvent e = new FilingEvent();
        e.filingId = filingId;
        e.sequence = sequence;
        e.eventType = type;
        return e;
    }

    public Long getId() {
        return id;
    }

    public Long getFilingId() {
        return filingId;
    }

    public Integer getSequence() {
        return sequence;
    }

    public FilingEventType getEventType() {
        return eventType;
    }

    public Long getEditFailureId() {
        return editFailureId;
    }

    public FilingEvent withEditFailureId(Long v) {
        this.editFailureId = v;
        return this;
    }

    public String getRuleId() {
        return ruleId;
    }

    public FilingEvent withRuleId(String v) {
        this.ruleId = v;
        return this;
    }

    public com.manas.regfiling.domain.EditSeverity getSeverity() {
        return severity;
    }

    public FilingEvent withSeverity(com.manas.regfiling.domain.EditSeverity v) {
        this.severity = v;
        return this;
    }

    public String getScheduleCode() {
        return scheduleCode;
    }

    public FilingEvent withScheduleCode(String v) {
        this.scheduleCode = v;
        return this;
    }

    public String getLineItemCode() {
        return lineItemCode;
    }

    public FilingEvent withLineItemCode(String v) {
        this.lineItemCode = v;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public FilingEvent withMessage(String v) {
        this.message = v;
        return this;
    }

    public com.manas.regfiling.domain.FilingStatus getStatusBefore() {
        return statusBefore;
    }

    public FilingEvent withStatusBefore(com.manas.regfiling.domain.FilingStatus v) {
        this.statusBefore = v;
        return this;
    }

    public com.manas.regfiling.domain.FilingStatus getStatusAfter() {
        return statusAfter;
    }

    public FilingEvent withStatusAfter(com.manas.regfiling.domain.FilingStatus v) {
        this.statusAfter = v;
        return this;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
