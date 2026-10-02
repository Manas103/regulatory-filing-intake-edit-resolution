package com.manas.regfiling.schema;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

/**
 * The declared schema for the one simulated report type this service understands, "SIM-Y9C": a
 * small slice of schedules modeled on the public FR Y-9C convention (HC balance sheet, HC-B
 * securities, HC-C loans and leases, RI income statement, RC-N past due and nonaccrual). This is
 * a synthetic convention invented for this project, not the real FR Y-9C form; see the README's
 * honest framing section.
 */
@Component
public class FilingSchemaCatalog {

    public static final String REPORT_TYPE = "SIM-Y9C";

    private static final List<DeclaredLineItem> LINE_ITEMS = List.of(
            new DeclaredLineItem("HC", "1.a", "Cash and balances due from depository institutions", true, true, false),
            new DeclaredLineItem("HC", "2.a", "Held-to-maturity securities, amortized cost", true, true, false),
            new DeclaredLineItem("HC", "2.b", "Available-for-sale securities, fair value", true, true, false),
            new DeclaredLineItem("HC", "4.a", "Loans and leases, net of unearned income", true, true, false),
            new DeclaredLineItem("HC", "12", "Total assets", true, true, false),
            new DeclaredLineItem("HC-B", "1", "Total held-to-maturity securities, amortized cost", true, true, false),
            new DeclaredLineItem("HC-B", "2", "Total available-for-sale securities, fair value", true, true, false),
            new DeclaredLineItem("HC-C", "1", "Loans secured by real estate", true, true, false),
            new DeclaredLineItem("HC-C", "2", "Commercial and industrial loans", true, true, false),
            new DeclaredLineItem("HC-C", "9", "Total loans and leases", true, true, false),
            new DeclaredLineItem("RI", "1", "Total interest income", true, true, false),
            new DeclaredLineItem("RI", "2", "Total interest expense", true, true, false),
            new DeclaredLineItem("RI", "3", "Net interest income", true, false, false),
            new DeclaredLineItem("RC-N", "1", "Loans and leases past due 30-89 days", true, true, true),
            new DeclaredLineItem("RC-N", "4", "Total nonaccrual loans and leases", true, true, true)
    );

    private static final Map<String, DeclaredLineItem> BY_KEY = LINE_ITEMS.stream()
            .collect(java.util.stream.Collectors.toMap(DeclaredLineItem::key, li -> li));

    private static final List<TieOutDefinition> TIE_OUTS = List.of(
            new TieOutDefinition("T1-TOTAL-ASSETS", "HC", "12", List.of(
                    new TieOutDefinition.LineItemRef("HC", "1.a"),
                    new TieOutDefinition.LineItemRef("HC", "2.a"),
                    new TieOutDefinition.LineItemRef("HC", "2.b"),
                    new TieOutDefinition.LineItemRef("HC", "4.a")
            )),
            new TieOutDefinition("T2-HTM-CROSS-SCHEDULE", "HC-B", "1", List.of(
                    new TieOutDefinition.LineItemRef("HC", "2.a")
            )),
            new TieOutDefinition("T3-AFS-CROSS-SCHEDULE", "HC-B", "2", List.of(
                    new TieOutDefinition.LineItemRef("HC", "2.b")
            )),
            new TieOutDefinition("T4-LOANS-SUBTOTAL", "HC-C", "9", List.of(
                    new TieOutDefinition.LineItemRef("HC-C", "1"),
                    new TieOutDefinition.LineItemRef("HC-C", "2")
            )),
            new TieOutDefinition("T5-LOANS-CROSS-SCHEDULE", "HC", "4.a", List.of(
                    new TieOutDefinition.LineItemRef("HC-C", "9")
            )),
            new TieOutDefinition("T6-NET-INTEREST-INCOME", "RI", "3",
                    List.of(new TieOutDefinition.LineItemRef("RI", "1")),
                    List.of(new TieOutDefinition.LineItemRef("RI", "2")))
    );

    public List<DeclaredLineItem> lineItems(String reportType) {
        if (!REPORT_TYPE.equals(reportType)) {
            return List.of();
        }
        return LINE_ITEMS;
    }

    public DeclaredLineItem find(String scheduleCode, String lineItemCode) {
        return BY_KEY.get(scheduleCode + ":" + lineItemCode);
    }

    public boolean isDeclared(String scheduleCode, String lineItemCode) {
        return BY_KEY.containsKey(scheduleCode + ":" + lineItemCode);
    }

    public List<TieOutDefinition> tieOuts(String reportType) {
        if (!REPORT_TYPE.equals(reportType)) {
            return List.of();
        }
        return TIE_OUTS;
    }
}
