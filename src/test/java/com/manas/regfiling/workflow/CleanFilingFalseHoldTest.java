package com.manas.regfiling.workflow;

import com.manas.regfiling.domain.FilingStatus;
import com.manas.regfiling.event.FilingEventStore;
import com.manas.regfiling.replay.FilingReplayProjector;
import com.manas.regfiling.repository.FilingRepository;
import com.manas.regfiling.schema.FilingSchemaCatalog;
import com.manas.regfiling.support.FilingLeafValuesBuilder;
import com.manas.regfiling.support.FilingTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The reference run behind "0 false holds over 200 clean synthetic filings": 200
 * internally-consistent filings (every tie-out holds by construction, every ratio and margin
 * randomized within the plausible band, no prior period so the two temporal quality rules never
 * fire), each submitted standalone with no injected defect. A single filing with no prior period
 * is used deliberately, so PERIOD_OVER_PERIOD_CHANGE and FLATLINED_VALUE (which both require
 * prior periods to compare against) cannot fire here by construction; the remaining seven rules
 * are fully exercised. Also folds in the per-filing slice of claim 5 (replay reproduces every
 * decision) for these 200 filings.
 */
@SpringBootTest
class CleanFilingFalseHoldTest {

    private static final int FILING_COUNT = 200;

    @Autowired
    private FilingWorkflowService workflowService;
    @Autowired
    private FilingRepository filingRepository;
    @Autowired
    private FilingEventStore eventStore;
    @Autowired
    private FilingReplayProjector replayProjector;

    @Test
    void twoHundredCleanFilingsProduceNoFalseHoldsAndReplayMatches() throws IOException {
        Random random = new Random(42);
        List<String> report = new ArrayList<>();
        int falseHolds = 0;
        int replayMatched = 0;

        for (int i = 0; i < FILING_COUNT; i++) {
            FilingLeafValuesBuilder b = new FilingLeafValuesBuilder()
                    .cash(jitter(random, 500_000, 0.2))
                    .htm(jitter(random, 200_000, 0.2))
                    .afs(jitter(random, 150_000, 0.2))
                    .loansRealEstate(jitter(random, 600_000, 0.2))
                    .loansCommercial(jitter(random, 300_000, 0.2))
                    .interestIncome(jitter(random, 50_000, 0.15))
                    .interestExpense(jitter(random, 20_000, 0.15))
                    .pastDue(jitter(random, 8_000, 0.2))
                    .nonaccrual(jitter(random, 5_000, 0.2));

            var filing = workflowService.createFiling("CLEAN-" + i, FilingSchemaCatalog.REPORT_TYPE,
                    LocalDate.of(2026, 7, 31), FilingTestFactory.toLineItems(b.build()));
            SubmissionResult result = workflowService.submit(filing.getId());

            if (!result.failures().isEmpty()) {
                falseHolds++;
                report.add("CLEAN-" + i + " -> " + result.failures().size() + " unexpected edit failure(s), status="
                        + result.status());
                for (var f : result.failures()) {
                    report.add("    " + f.getRuleId() + " at " + f.getScheduleCode() + ":" + f.getLineItemCode()
                            + ": " + f.getMessage());
                }
            }

            FilingStatus replayed = replayProjector.replay(eventStore.findByFilingId(filing.getId()));
            FilingStatus live = filingRepository.findById(filing.getId()).orElseThrow().getStatus();
            if (replayed == live) {
                replayMatched++;
            } else {
                report.add("CLEAN-" + i + " -> REPLAY MISMATCH live=" + live + " replayed=" + replayed);
            }
        }

        report.add("");
        report.add("False holds: " + falseHolds + " of " + FILING_COUNT);
        report.add("Replay matched live status: " + replayMatched + " of " + FILING_COUNT);

        Files.createDirectories(Path.of("docs"));
        Files.write(Path.of("docs", "clean_filing_false_hold_output.txt"), report, StandardCharsets.UTF_8);

        assertEquals(0, falseHolds, "see docs/clean_filing_false_hold_output.txt for the unexpected holds");
        assertEquals(FILING_COUNT, replayMatched, "see docs/clean_filing_false_hold_output.txt for replay mismatches");
    }

    private static long jitter(Random random, long base, double fraction) {
        double delta = (random.nextDouble() * 2 - 1) * fraction;
        return Math.round(base * (1 + delta));
    }
}
