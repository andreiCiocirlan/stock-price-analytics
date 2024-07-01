package com.example.stockprices.model.annual;

//@Entity
public class EbitPerShareItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "EbitPerShareItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}