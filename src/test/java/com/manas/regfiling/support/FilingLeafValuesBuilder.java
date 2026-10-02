package com.manas.regfiling.support;

import java.math.BigDecimal;

/** Fluent builder over {@link FilingLeafValues}, starting from the baseline, so a test can
 * override exactly the one or two leaves a seeded defect needs and leave the rest consistent. */
public final class FilingLeafValuesBuilder {

    private BigDecimal cashHc1a;
    private BigDecimal htmHc2a;
    private BigDecimal afsHc2b;
    private BigDecimal loansRealEstateHcC1;
    private BigDecimal loansCommercialHcC2;
    private BigDecimal interestIncomeRi1;
    private BigDecimal interestExpenseRi2;
    private BigDecimal pastDueRcN1;
    private BigDecimal nonaccrualRcN4;

    public FilingLeafValuesBuilder() {
        FilingLeafValues b = FilingLeafValues.baseline();
        cashHc1a = b.cashHc1a();
        htmHc2a = b.htmHc2a();
        afsHc2b = b.afsHc2b();
        loansRealEstateHcC1 = b.loansRealEstateHcC1();
        loansCommercialHcC2 = b.loansCommercialHcC2();
        interestIncomeRi1 = b.interestIncomeRi1();
        interestExpenseRi2 = b.interestExpenseRi2();
        pastDueRcN1 = b.pastDueRcN1();
        nonaccrualRcN4 = b.nonaccrualRcN4();
    }

    public FilingLeafValuesBuilder cash(long v) {
        this.cashHc1a = BigDecimal.valueOf(v);
        return this;
    }

    public FilingLeafValuesBuilder htm(long v) {
        this.htmHc2a = BigDecimal.valueOf(v);
        return this;
    }

    public FilingLeafValuesBuilder afs(long v) {
        this.afsHc2b = BigDecimal.valueOf(v);
        return this;
    }

    public FilingLeafValuesBuilder loansRealEstate(long v) {
        this.loansRealEstateHcC1 = BigDecimal.valueOf(v);
        return this;
    }

    public FilingLeafValuesBuilder loansCommercial(long v) {
        this.loansCommercialHcC2 = BigDecimal.valueOf(v);
        return this;
    }

    public FilingLeafValuesBuilder interestIncome(long v) {
        this.interestIncomeRi1 = BigDecimal.valueOf(v);
        return this;
    }

    public FilingLeafValuesBuilder interestExpense(long v) {
        this.interestExpenseRi2 = BigDecimal.valueOf(v);
        return this;
    }

    public FilingLeafValuesBuilder pastDue(long v) {
        this.pastDueRcN1 = BigDecimal.valueOf(v);
        return this;
    }

    public FilingLeafValuesBuilder nonaccrual(long v) {
        this.nonaccrualRcN4 = BigDecimal.valueOf(v);
        return this;
    }

    public FilingLeafValues build() {
        return new FilingLeafValues(cashHc1a, htmHc2a, afsHc2b, loansRealEstateHcC1, loansCommercialHcC2,
                interestIncomeRi1, interestExpenseRi2, pastDueRcN1, nonaccrualRcN4);
    }
}
