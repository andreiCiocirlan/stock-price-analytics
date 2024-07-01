package com.example.stockprices.model.annual;

//@Entity
public class TotalRatioItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "TotalRatioItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}