package com.manas.regfiling.web;

import com.manas.regfiling.domain.Filing;
import com.manas.regfiling.repository.EditFailureRepository;
import com.manas.regfiling.repository.FilingRepository;
import com.manas.regfiling.workflow.FilingWorkflowService;
import com.manas.regfiling.workflow.LineItemInput;
import com.manas.regfiling.workflow.SubmissionResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

/** The REST surface the React console talks to. Every claim this project measures is proved in
 * the test suite at the service layer; this controller is a thin, directly testable adapter. */
@RestController
@RequestMapping("/api/filings")
public class FilingController {

    private final FilingWorkflowService workflowService;
    private final FilingRepository filingRepository;
    private final EditFailureRepository editFailureRepository;

    public FilingController(FilingWorkflowService workflowService, FilingRepository filingRepository,
                             EditFailureRepository editFailureRepository) {
        this.workflowService = workflowService;
        this.filingRepository = filingRepository;
        this.editFailureRepository = editFailureRepository;
    }

    @PostMapping
    public ResponseEntity<FilingView> create(@RequestBody CreateFilingRequest request) {
        Filing filing = workflowService.createFiling(request.institutionId(), request.reportType(),
                request.periodEndDate(),
                request.lineItems().stream()
                        .map(li -> new LineItemInput(li.scheduleCode(), li.lineItemCode(), li.rawValue()))
                        .toList());
        return ResponseEntity.ok(FilingView.of(filing, editFailureRepository.findByFilingId(filing.getId())));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<FilingView> submit(@PathVariable Long id) {
        SubmissionResult result = workflowService.submit(id);
        Filing filing = filingRepository.findById(id).orElseThrow(() -> new NoSuchElementException("No filing " + id));
        return ResponseEntity.ok(FilingView.of(filing, result.failures()));
    }

    @PostMapping("/{id}/explanations")
    public ResponseEntity<FilingView> explain(@PathVariable Long id, @RequestBody ExplanationRequest request) {
        Filing filing = workflowService.addExplanation(id, request.editFailureId(), request.explanation());
        return ResponseEntity.ok(FilingView.of(filing, editFailureRepository.findByFilingId(id)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FilingView> get(@PathVariable Long id) {
        Filing filing = filingRepository.findById(id).orElseThrow(() -> new NoSuchElementException("No filing " + id));
        return ResponseEntity.ok(FilingView.of(filing, editFailureRepository.findByFilingId(id)));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.notFound().build();
    }
}
