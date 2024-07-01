package com.example.stockprices.model.annual;

//@Entity
public class FcfMarginItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "FcfMarginItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}