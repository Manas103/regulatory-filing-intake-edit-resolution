package com.manas.regfiling.workflow;

import com.manas.regfiling.domain.EditFailure;
import com.manas.regfiling.domain.EditSeverity;
import com.manas.regfiling.domain.Filing;
import com.manas.regfiling.domain.FilingStatus;
import com.manas.regfiling.domain.LineItemValue;
import com.manas.regfiling.event.FilingEvent;
import com.manas.regfiling.event.FilingEventStore;
import com.manas.regfiling.event.FilingEventType;
import com.manas.regfiling.repository.EditFailureRepository;
import com.manas.regfiling.repository.FilingRepository;
import com.manas.regfiling.repository.LineItemValueRepository;
import com.manas.regfiling.rules.EditEngine;
import com.manas.regfiling.rules.EditFailureDraft;
import com.manas.regfiling.rules.FilingEvaluationContext;
import com.manas.regfiling.rules.PriorPeriod;
import com.manas.regfiling.schema.FilingSchemaCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * The respondent submission workflow: DRAFT -> SUBMITTED -> (REJECTED_VALIDITY terminal) or
 * (HELD_FOR_EXPLANATION until every quality edit that fired has an explanation, then ACCEPTED)
 * or (ACCEPTED directly when no edit fired at all). Every transition, every edit evaluation and
 * every explanation is appended to the event log via {@link FilingEventStore} in the same
 * transaction as the materialized {@code Filing.status} column update, so the two are always
 * consistent as of any commit; {@link com.manas.regfiling.replay.FilingReplayProjector} exists
 * to prove the log alone is sufficient without trusting that column.
 */
@Service
public class FilingWorkflowService {

    private final FilingRepository filingRepository;
    private final LineItemValueRepository lineItemValueRepository;
    private final EditFailureRepository editFailureRepository;
    private final FilingEventStore eventStore;
    private final EditEngine editEngine;
    private final FilingSchemaCatalog catalog;

    public FilingWorkflowService(FilingRepository filingRepository,
                                  LineItemValueRepository lineItemValueRepository,
                                  EditFailureRepository editFailureRepository,
                                  FilingEventStore eventStore,
                                  EditEngine editEngine,
                                  FilingSchemaCatalog catalog) {
        this.filingRepository = filingRepository;
        this.lineItemValueRepository = lineItemValueRepository;
        this.editFailureRepository = editFailureRepository;
        this.eventStore = eventStore;
        this.editEngine = editEngine;
        this.catalog = catalog;
    }

    @Transactional
    public Filing createFiling(String institutionId, String reportType, java.time.LocalDate periodEndDate,
                                List<LineItemInput> values) {
        Filing filing = filingRepository.save(new Filing(institutionId, reportType, periodEndDate));
        for (LineItemInput v : values) {
            lineItemValueRepository.save(new LineItemValue(filing.getId(), v.scheduleCode(), v.lineItemCode(), v.rawValue()));
        }
        appendEvent(filing.getId(), FilingEventType.FILING_CREATED, null);
        return filing;
    }

    @Transactional
    public SubmissionResult submit(Long filingId) {
        Filing filing = filingRepository.findById(filingId)
                .orElseThrow(() -> new NoSuchElementException("No filing " + filingId));
        FilingStatus before = filing.getStatus();
        filing.setStatus(FilingStatus.SUBMITTED);
        appendEvent(filingId, FilingEventType.FILING_SUBMITTED, e -> e.withStatusBefore(before).withStatusAfter(FilingStatus.SUBMITTED));

        List<LineItemValue> currentValues = lineItemValueRepository.findByFilingId(filingId);
        List<PriorPeriod> priorPeriods = loadPriorPeriods(filing);
        FilingEvaluationContext ctx = new FilingEvaluationContext(filing, currentValues, catalog, priorPeriods);

        List<EditFailureDraft> drafts = editEngine.evaluate(ctx);
        boolean hadValidity = false;
        List<EditFailure> persisted = new java.util.ArrayList<>();
        for (EditFailureDraft draft : drafts) {
            EditFailure failure = editFailureRepository.save(new EditFailure(filingId, draft.ruleId().name(),
                    draft.ruleId().severity(), draft.scheduleCode(), draft.lineItemCode(), draft.message()));
            persisted.add(failure);
            if (draft.ruleId().severity() == EditSeverity.VALIDITY) {
                hadValidity = true;
            }
            appendEvent(filingId, FilingEventType.EDIT_FAILURE_RECORDED, e -> e
                    .withEditFailureId(failure.getId())
                    .withRuleId(draft.ruleId().name())
                    .withSeverity(draft.ruleId().severity())
                    .withScheduleCode(draft.scheduleCode())
                    .withLineItemCode(draft.lineItemCode())
                    .withMessage(draft.message()));
        }

        FilingStatus statusBeforeDecision = filing.getStatus();
        FilingStatus finalStatus;
        if (hadValidity) {
            finalStatus = FilingStatus.REJECTED_VALIDITY;
        } else {
            boolean anyQuality = persisted.stream().anyMatch(f -> f.getSeverity() == EditSeverity.QUALITY);
            finalStatus = anyQuality ? FilingStatus.HELD_FOR_EXPLANATION : FilingStatus.ACCEPTED;
        }
        filing.setStatus(finalStatus);
        appendEvent(filingId, FilingEventType.STATUS_CHANGED,
                e -> e.withStatusBefore(statusBeforeDecision).withStatusAfter(finalStatus));

        return new SubmissionResult(finalStatus, persisted);
    }

    @Transactional
    public Filing addExplanation(Long filingId, Long editFailureId, String explanationText) {
        Filing filing = filingRepository.findById(filingId)
                .orElseThrow(() -> new NoSuchElementException("No filing " + filingId));
        EditFailure failure = editFailureRepository.findById(editFailureId)
                .orElseThrow(() -> new NoSuchElementException("No edit failure " + editFailureId));
        if (!failure.getFilingId().equals(filingId)) {
            throw new IllegalArgumentException("Edit failure " + editFailureId + " does not belong to filing " + filingId);
        }
        if (failure.getSeverity() != EditSeverity.QUALITY) {
            throw new IllegalStateException("Only QUALITY edit failures can be explained, " + editFailureId + " is VALIDITY");
        }
        failure.setExplanation(explanationText);
        failure.setExplainedAt(java.time.Instant.now());
        editFailureRepository.save(failure);

        appendEvent(filingId, FilingEventType.EXPLANATION_ADDED, e -> e
                .withEditFailureId(editFailureId)
                .withScheduleCode(failure.getScheduleCode())
                .withLineItemCode(failure.getLineItemCode())
                .withMessage(explanationText));

        List<EditFailure> allFailures = editFailureRepository.findByFilingId(filingId);
        boolean allQualityExplained = allFailures.stream()
                .filter(f -> f.getSeverity() == EditSeverity.QUALITY)
                .allMatch(EditFailure::isExplained);

        if (filing.getStatus() == FilingStatus.HELD_FOR_EXPLANATION && allQualityExplained) {
            FilingStatus before = filing.getStatus();
            filing.setStatus(FilingStatus.ACCEPTED);
            appendEvent(filingId, FilingEventType.STATUS_CHANGED,
                    e -> e.withStatusBefore(before).withStatusAfter(FilingStatus.ACCEPTED));
        }
        return filing;
    }

    private List<PriorPeriod> loadPriorPeriods(Filing filing) {
        List<Filing> priors = filingRepository
                .findByInstitutionIdAndReportTypeAndPeriodEndDateLessThanOrderByPeriodEndDateDesc(
                        filing.getInstitutionId(), filing.getReportType(), filing.getPeriodEndDate());
        List<PriorPeriod> result = new java.util.ArrayList<>();
        for (Filing prior : priors) {
            result.add(new PriorPeriod(prior.getPeriodEndDate(), lineItemValueRepository.findByFilingId(prior.getId())));
        }
        return result;
    }

    private void appendEvent(Long filingId, FilingEventType type,
                              java.util.function.UnaryOperator<FilingEvent> customize) {
        int seq = eventStore.nextSequence(filingId);
        FilingEvent event = FilingEvent.of(filingId, seq, type);
        if (customize != null) {
            event = customize.apply(event);
        }
        eventStore.append(event);
    }
}
