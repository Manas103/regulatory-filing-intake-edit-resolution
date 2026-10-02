package com.manas.regfiling.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A single regulatory report filing for one institution, one report type, one period. The
 * {@code status} column is the materialized, "current status" view used for serving (fast reads
 * for the console and the API). It is deliberately NOT the source of truth: the append only
 * {@code filing_event} table is. {@link com.manas.regfiling.replay.FilingReplayProjector} proves
 * the two agree by recomputing status from the event log alone, independent of this column.
 */
@Entity
@Table(name = "filing")
public class Filing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String institutionId;

    @Column(nullable = false)
    private String reportType;

    @Column(nullable = false)
    private LocalDate periodEndDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FilingStatus status = FilingStatus.DRAFT;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Filing() {
    }

    public Filing(String institutionId, String reportType, LocalDate periodEndDate) {
        this.institutionId = institutionId;
        this.reportType = reportType;
        this.periodEndDate = periodEndDate;
    }

    public Long getId() {
        return id;
    }

    public String getInstitutionId() {
        return institutionId;
    }

    public String getReportType() {
        return reportType;
    }

    public LocalDate getPeriodEndDate() {
        return periodEndDate;
    }

    public FilingStatus getStatus() {
        return status;
    }

    public void setStatus(FilingStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
