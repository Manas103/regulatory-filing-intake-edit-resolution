package com.manas.regfiling.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * One fired edit on one filing. Every row carries the exact schedule_code and line_item_code it
 * fired against, the rule id, and a human readable message. A QUALITY failure can later receive
 * an analyst explanation; a VALIDITY failure never gets one, it already blocked the submission.
 */
@Entity
@Table(name = "edit_failure")
public class EditFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long filingId;

    @Column(nullable = false)
    private String ruleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EditSeverity severity;

    @Column(nullable = false)
    private String scheduleCode;

    @Column(nullable = false)
    private String lineItemCode;

    @Column(nullable = false)
    private String message;

    @Column
    private String explanation;

    @Column
    private Instant explainedAt;

    protected EditFailure() {
    }

    public EditFailure(Long filingId, String ruleId, EditSeverity severity, String scheduleCode,
                        String lineItemCode, String message) {
        this.filingId = filingId;
        this.ruleId = ruleId;
        this.severity = severity;
        this.scheduleCode = scheduleCode;
        this.lineItemCode = lineItemCode;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public Long getFilingId() {
        return filingId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public EditSeverity getSeverity() {
        return severity;
    }

    public String getScheduleCode() {
        return scheduleCode;
    }

    public String getLineItemCode() {
        return lineItemCode;
    }

    public String getMessage() {
        return message;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Instant getExplainedAt() {
        return explainedAt;
    }

    public void setExplainedAt(Instant explainedAt) {
        this.explainedAt = explainedAt;
    }

    public boolean isExplained() {
        return explanation != null;
    }
}
