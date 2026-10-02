package com.manas.regfiling.support;

import com.manas.regfiling.workflow.LineItemInput;

import java.util.ArrayList;
import java.util.List;

/** Turns {@link FilingLeafValues} into the 15 declared SIM-Y9C line items, consistent by
 * construction, plus small mutation helpers tests use to inject exactly one defect. */
public final class FilingTestFactory {

    private FilingTestFactory() {
    }

    public static List<LineItemInput> toLineItems(FilingLeafValues v) {
        List<LineItemInput> items = new ArrayList<>();
        items.add(li("HC", "1.a", v.cashHc1a()));
        items.add(li("HC", "2.a", v.htmHc2a()));
        items.add(li("HC", "2.b", v.afsHc2b()));
        items.add(li("HC", "4.a", v.totalLoansHcC9()));
        items.add(li("HC", "12", v.totalAssetsHc12()));
        items.add(li("HC-B", "1", v.htmHc2a()));
        items.add(li("HC-B", "2", v.afsHc2b()));
        items.add(li("HC-C", "1", v.loansRealEstateHcC1()));
        items.add(li("HC-C", "2", v.loansCommercialHcC2()));
        items.add(li("HC-C", "9", v.totalLoansHcC9()));
        items.add(li("RI", "1", v.interestIncomeRi1()));
        items.add(li("RI", "2", v.interestExpenseRi2()));
        items.add(li("RI", "3", v.netInterestIncomeRi3()));
        items.add(li("RC-N", "1", v.pastDueRcN1()));
        items.add(li("RC-N", "4", v.nonaccrualRcN4()));
        return items;
    }

    private static LineItemInput li(String schedule, String line, java.math.BigDecimal value) {
        return new LineItemInput(schedule, line, value.toPlainString());
    }

    public static List<LineItemInput> removeItem(List<LineItemInput> items, String schedule, String line) {
        List<LineItemInput> copy = new ArrayList<>(items);
        copy.removeIf(i -> i.scheduleCode().equals(schedule) && i.lineItemCode().equals(line));
        return copy;
    }

    public static List<LineItemInput> setRawValue(List<LineItemInput> items, String schedule, String line, String newValue) {
        List<LineItemInput> copy = new ArrayList<>();
        for (LineItemInput i : items) {
            if (i.scheduleCode().equals(schedule) && i.lineItemCode().equals(line)) {
                copy.add(new LineItemInput(schedule, line, newValue));
            } else {
                copy.add(i);
            }
        }
        return copy;
    }

    public static List<LineItemInput> addItem(List<LineItemInput> items, String schedule, String line, String value) {
        List<LineItemInput> copy = new ArrayList<>(items);
        copy.add(new LineItemInput(schedule, line, value));
        return copy;
    }
}
