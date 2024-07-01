package com.example.stockprices.model.annual;

//@Entity
public class RoaItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "RoaItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}