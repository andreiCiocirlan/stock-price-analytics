package com.example.stockprices.model.annual;

//@Entity
public class PfcfItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "PfcfItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}