package com.example.stockprices.model.annual;

//@Entity
public class OperatingMarginItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "OperatingMarginItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}