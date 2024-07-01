package com.example.stockprices.model.annual;

//@Entity
public class LongtermDebtTotalCapitalItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "LongtermDebtTotalCapitalItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}