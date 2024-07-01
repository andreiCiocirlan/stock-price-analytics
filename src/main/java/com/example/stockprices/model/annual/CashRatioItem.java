package com.example.stockprices.model.annual;

//@Entity
public class CashRatioItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "CashRatioItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}