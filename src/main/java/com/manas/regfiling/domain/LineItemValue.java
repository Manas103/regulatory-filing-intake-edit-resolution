package com.manas.regfiling.domain;

import jakarta.persistence.*;

/**
 * One (schedule_code, line_item_code) value on a filing. The raw value is kept as text on
 * purpose: a defective filing can submit a non-numeric value for a line item that is supposed
 * to be numeric, and the type check rule needs to see that raw text to fail it correctly rather
 * than have a column type silently coerce or reject it before the edit engine ever runs.
 */
@Entity
@Table(name = "line_item_value")
public class LineItemValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long filingId;

    @Column(nullable = false)
    private String scheduleCode;

    @Column(nullable = false)
    private String lineItemCode;

    @Column(nullable = false)
    private String rawValue;

    protected LineItemValue() {
    }

    public LineItemValue(Long filingId, String scheduleCode, String lineItemCode, String rawValue) {
        this.filingId = filingId;
        this.scheduleCode = scheduleCode;
        this.lineItemCode = lineItemCode;
        this.rawValue = rawValue;
    }

    public Long getId() {
        return id;
    }

    public Long getFilingId() {
        return filingId;
    }

    public String getScheduleCode() {
        return scheduleCode;
    }

    public String getLineItemCode() {
        return lineItemCode;
    }

    public String getRawValue() {
        return rawValue;
    }
}
