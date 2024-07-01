package com.example.stockprices.model.annual;

//@Entity
public class RoicItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "RoicItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}