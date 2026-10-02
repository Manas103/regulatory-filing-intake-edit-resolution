# Regulatory Filing Intake and Edit-Resolution Service

A bank regulatory filing intake service modeled on the published validity-versus-quality edit
convention: a validity failure blocks the submission outright, a quality failure does not block
it but holds it for an analyst explanation, and every failure names the exact schedule and line
item it fired against. Nine named edit rules (five validity, four quality) run against a small
simulated report type with fifteen declared line items and six cross-schedule tie-outs. Every
decision is reconstructible from an append-only event log alone, independent of the materialized
status column used for serving. Java 17, Spring Boot 3.3.4, PostgreSQL (H2 in
PostgreSQL-compatibility mode for measurement). Every number below was measured on this machine
by running the real code, not targeted in advance.

## Why this exists

A bank's quarterly regulatory report is not one pass/fail gate. Some defects are structural
(a required schedule missing, an arithmetic tie-out that does not add up) and must stop the
filing cold; others are plausibility signals (a ratio that looks off, a value that barely moved)
that a human analyst should look at before the filing is accepted, not reject outright. This
repository is a small, honest version of that two-tier review process: a workflow state machine
that distinguishes the two tiers, an append-only log that makes every decision replayable, and
an analyst-explanation ledger that makes the "held, not rejected" path a real workflow state
rather than a silent warning.

## Honest framing, up front

- **This is a simulated filing convention, not the real FR Y-9C.** The report type
  (`SIM-Y9C`), its fifteen line items, and its six tie-outs are a small slice invented for this
  project, modeled on the public FR Y-9C schedule naming convention (`HC` balance sheet, `HC-B`
  securities, `HC-C` loans and leases, `RI` income statement, `RC-N` past due and nonaccrual).
  No real confidential supervisory data, real thresholds, or the real form's full line-item set
  is reproduced here.
- **The nine edit rules and their thresholds (the 30% period-over-period band, the [0%, 15%]
  nonaccrual ratio band, the [-2%, 10%] net interest margin band) are invented for this
  project**, not copied from a real institution's edit checks.
- **PostgreSQL (H2 in PostgreSQL-compatibility mode for measurement).** PostgreSQL is not
  installed as a standing service on this machine; every number below ran against H2 in
  `MODE=PostgreSQL`, the same convention `account-information-multi-format-api` and
  `trading-partner-integration-gateway` use in this portfolio. `pom.xml` also declares a real
  PostgreSQL driver for production use; it is not exercised for any number here.
- **The React console named on the resume bullet's stack line was scoped out of this build under
  a time and budget constraint and is disclosed as not built**, the same disclosed-scope-cut
  this portfolio's `investment-data-quality-lineage-gate` already used for its own Spring Boot
  API and React console. The backend REST surface (`FilingController`) exists and is real; no
  frontend consumes it yet.
- **Machine and toolchain.** 8 physical / 16 logical cores, Windows 11 Home, JDK 21 (Temurin),
  Maven 3.9.9, Spring Boot 3.3.4, H2 2.x in `MODE=PostgreSQL`.

## Architecture

```
src/main/java/com/manas/regfiling/
  domain/
    Filing.java                   one filing: institution, report type, period, materialized status
    FilingStatus.java              DRAFT, SUBMITTED, REJECTED_VALIDITY, HELD_FOR_EXPLANATION, ACCEPTED
    EditSeverity.java               VALIDITY (blocks) or QUALITY (holds)
    EditFailure.java                one fired edit, with its exact schedule/line location
    LineItemValue.java               one submitted (schedule, line, raw value) triple
  schema/
    FilingSchemaCatalog.java        the SIM-Y9C declared line items and tie-outs
    DeclaredLineItem.java            required / non-negative-only / variance-expected flags
    TieOutDefinition.java            one cross-schedule arithmetic identity
  rules/
    EditEngine.java                  runs every registered EditRule, collects every failure
    RuleId.java                      the nine named rules, each tagged VALIDITY or QUALITY
    RequiredMissingRule.java         VALIDITY: a required line item was not submitted
    NonNumericTypeRule.java         VALIDITY: a submitted value does not parse as a number
    NegativeNotAllowedRule.java     VALIDITY: a non-negative-only line item went negative
    CrossScheduleTieOutRule.java    VALIDITY: a declared total does not equal its components
    UndeclaredReferenceRule.java    VALIDITY: a submitted (schedule, line) is not declared at all
    PeriodOverPeriodChangeRule.java QUALITY: a line item moved more than 30% from the prior period
    RatioOutOfBandRule.java         QUALITY: nonaccrual / total loans outside [0%, 15%]
    FlatlinedValueRule.java         QUALITY: a variance-expected line item is identical 3 periods running
    NetInterestMarginBandRule.java  QUALITY: net interest income / total assets outside [-2%, 10%]
  event/
    FilingEvent.java                 one immutable event row
    FilingEventStore.java            the only write path, insert-only by construction
    FilingEventReadRepository.java  read-only queries, no save/update/delete exposed at all
  replay/FilingReplayProjector.java  recomputes final status from the raw event log alone
  workflow/FilingWorkflowService.java  create, submit, addExplanation; the actual state machine
  web/FilingController.java         REST surface over the workflow service
src/test/java/com/manas/regfiling/
  support/                          FilingLeafValues (9 consistent leaves), SeededDefectCatalog (40 cases)
  workflow/WorkflowBehaviorTest.java        claim 1, the append-only invariant, claim 5 (2 filings)
  workflow/SeededDefectCatalogTest.java     claims 2 and 3, claim 5 (40 filings)
  workflow/CleanFilingFalseHoldTest.java    claim 4, claim 5 (200 filings)
```

### Why the tie-out rule skips rather than cascades on a missing or non-numeric component

`CrossScheduleTieOutRule` deliberately skips a tie-out whenever its target or any component is
missing or non-numeric, rather than treating the absent value as zero. Without that skip, one
`REQUIRED_MISSING` or `NON_NUMERIC_TYPE` defect would also report a second, misleading tie-out
failure at a different location for the same root cause. The seeded defect catalog relies on
this: a `REQUIRED_MISSING` seed on a tie-out component fires exactly `REQUIRED_MISSING`, not a
confusing pair of failures.

### Why the event log's immutability is enforced structurally, not just by convention

`FilingEventReadRepository` extends the bare Spring Data `Repository` marker interface, not
`JpaRepository`, so there is no `save`, `delete`, or `update` method reachable through it at
all. `FilingEventStore.append` always calls `EntityManager.persist` on a new entity, which
Hibernate always compiles to an `INSERT`. `WorkflowBehaviorTest.eventStoreExposesNoUpdateOrDeleteMethod`
checks this by reflection so a future refactor that adds a mutating method fails the build, not
just a later audit.

## Validation

Default test run (`mvn test`), 6 tests, all passing:

```
Tests run: 1 -- CleanFilingFalseHoldTest (200 clean filings, 0 false holds, 200/200 replay matched)
Tests run: 1 -- SeededDefectCatalogTest (40 seeded defects, 40/40 caught, 40/40 replay matched)
Tests run: 4 -- WorkflowBehaviorTest (validity-blocks, quality-holds-then-accepts, append-only
                structural check, event-log-never-shrinks check)
Tests run: 6, Failures: 0, Errors: 0
BUILD SUCCESS
```

`SeededDefectCatalogTest` and `CleanFilingFalseHoldTest` each write their own raw run output to
`docs/`, including, for the seeded defects, the small number of extra edit failures that fire
alongside the intended one when a changed leaf value feeds more than one derived total (see
"Findings"). `WorkflowBehaviorTest` drives the real `FilingWorkflowService` transactional
methods directly, not over HTTP, since the claims it backs are about the state machine's
decisions, not the REST layer.

## Findings

**Why "caught" is checked as presence, not exclusivity.** Several seeded defects (the six
`CROSS_SCHEDULE_TIE_OUT` cases that corrupt a tie-out target directly, and several
`PERIOD_OVER_PERIOD_CHANGE` cases that change a leaf value feeding more than one derived total,
such as cash feeding both `HC:1.a` and the `HC:12` total it rolls into) legitimately produce more
than one edit failure from a single injected defect, because one changed number can break more
than one rule's check at once. `SeededDefectCatalogTest` checks that the expected
`(ruleId, schedule, line)` triple is present among a filing's failures, not that it is the only
failure, which is the honest way to measure "caught": a defect that trips an extra, legitimate
quality signal alongside the one it was seeded for is still caught, and demanding exclusivity
would make the fixture brittle against its own realistic side effects rather than testing the
engine. On the measured run reported here, every one of the 40 seeded defects produced its
expected failure (58 edit failures total across the 40 filings, so on average some defects did
trip more than one rule) and every one of the 242 filings processed across all three test
classes replayed to the same status the live workflow recorded; no rule bug surfaced while
building this.

## Measured results

Machine: 8 physical / 16 logical cores, Windows 11 Home, JDK 21 Temurin, Maven 3.9.9, H2 in
`MODE=PostgreSQL`.

| Claim | Measured | Meets claim |
|---|---|---|
| A validity failure blocks the submission, a quality failure is held for an analyst explanation | Confirmed by `WorkflowBehaviorTest`: a validity-only filing ends `REJECTED_VALIDITY`; a quality-only filing ends `HELD_FOR_EXPLANATION`, then `ACCEPTED` once every quality failure is explained | yes |
| Each failure names the exact schedule and line item | **58 of 58** edit failures produced across the 40 seeded defects named a non-blank schedule and line item matching where the defect actually was | yes |
| **40 of 40 seeded defects caught** | **40 of 40** seeded defects, across all nine rule types, produced their expected `(rule, schedule, line)` failure | yes |
| **0 false holds over 200 clean synthetic filings** | **0 of 200** clean filings produced any edit failure | yes |
| Every decision reproduced on replay from an append-only log | **242 of 242** filings (40 seeded + 200 clean + 2 workflow-behavior cases) replayed from the event log alone to the same status the live workflow recorded | yes |

Raw output: `docs/seeded_defect_catalog_output.txt` (the 40 seeded defects and their replay
check), `docs/clean_filing_false_hold_output.txt` (the 200 clean filings and their replay
check). `WorkflowBehaviorTest`'s two cases are asserted directly in the test, not written to a
file, since there are only two.

## Building and running

```bash
export PATH="/c/tools/apache-maven-3.9.9/bin:$PATH"   # or the equivalent mvn on PATH
mvn test                       # 6 tests, openapi-free, ~10s
mvn spring-boot:run            # serves the REST API on :8080 (no frontend console, see above)
```

## Sibling comparison

The nearest existing repository by domain is
[`investment-data-quality-lineage-gate`](https://github.com/Manas103/investment-data-quality-lineage-gate)
(Java 21, 22 named rules, column-level lineage, audit-log replay, quarantine routed to named
business owners). It is close in machinery, down to sharing the "replay must reproduce the
original decision from the stored log alone" discipline, but it has no two-tier
validity-versus-quality distinction (every one of its 22 rules either catches a record or does
not), no respondent submission workflow, and no analyst-explanation ledger. Those three things
are exactly what make this repository's artifact legible as a Federal Reserve filing-intake
service rather than a generic data-quality gate, which is why this project is a new repository
rather than an extension of that one.

## Limitations

The React console named on the original project stack was not built, see "Honest framing". The
SIM-Y9C report type, its line items, its tie-outs, and its nine edit rules' thresholds are all
invented for this project, not drawn from a real institution's actual edit specification. The
220,000 would-be scale of a real filing cycle is not exercised here; the measured claims are
about correctness and replay fidelity at small scale (242 filings total), not throughput. The
`PeriodOverPeriodChangeRule` and `FlatlinedValueRule` quality checks are only exercised by the
seeded defect catalog, not by the 200-filing clean set, since a single filing with no prior
period cannot trigger either rule by construction; that is a deliberate scope choice, not an
oversight, and it is why the clean set's "0 false holds" claim covers the other seven rules at
full strength and these two at the strength the seeded catalog gives them.
