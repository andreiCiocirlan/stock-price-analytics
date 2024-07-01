package com.example.stockprices.model.annual;

//@Entity
public class PretaxMarginItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "PretaxMarginItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}