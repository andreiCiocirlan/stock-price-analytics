package com.example.stockprices.model.annual;

//@Entity
public class TotalDebtToTotalCapitalItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "TotalDebtToTotalCapitalItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}