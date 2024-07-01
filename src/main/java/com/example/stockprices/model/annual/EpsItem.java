package com.example.stockprices.model.annual;

//@Entity
public class EpsItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "EpsItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}