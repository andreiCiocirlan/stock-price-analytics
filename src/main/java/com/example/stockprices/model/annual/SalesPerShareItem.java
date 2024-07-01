package com.example.stockprices.model.annual;

//@Entity
public class SalesPerShareItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "SalesPerShareItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}