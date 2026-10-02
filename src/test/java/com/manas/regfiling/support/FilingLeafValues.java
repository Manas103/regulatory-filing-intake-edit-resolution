package com.manas.regfiling.support;

import java.math.BigDecimal;

/**
 * The nine independent leaf inputs a clean, internally consistent SIM-Y9C filing needs. Every
 * other declared line item (the four totals and net interest income) is derived from these so
 * that every tie-out holds by construction unless a test deliberately overrides one afterward.
 */
public record FilingLeafValues(
        BigDecimal cashHc1a,
        BigDecimal htmHc2a,
        BigDecimal afsHc2b,
        BigDecimal loansRealEstateHcC1,
        BigDecimal loansCommercialHcC2,
        BigDecimal interestIncomeRi1,
        BigDecimal interestExpenseRi2,
        BigDecimal pastDueRcN1,
        BigDecimal nonaccrualRcN4
) {
    public static FilingLeafValues baseline() {
        return new FilingLeafValues(
                new BigDecimal("500000"),
                new BigDecimal("200000"),
                new BigDecimal("150000"),
                new BigDecimal("600000"),
                new BigDecimal("300000"),
                new BigDecimal("50000"),
                new BigDecimal("20000"),
                new BigDecimal("8000"),
                new BigDecimal("5000")
        );
    }

    public BigDecimal totalLoansHcC9() {
        return loansRealEstateHcC1.add(loansCommercialHcC2);
    }

    public BigDecimal totalAssetsHc12() {
        return cashHc1a.add(htmHc2a).add(afsHc2b).add(totalLoansHcC9());
    }

    public BigDecimal netInterestIncomeRi3() {
        return interestIncomeRi1.subtract(interestExpenseRi2);
    }
}
