package com.example.stockprices.model.annual;

//@Entity
public class PtbvItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "PtbvItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}