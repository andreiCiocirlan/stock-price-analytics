package com.example.stockprices.model.annual;

//@Entity
public class PeItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "PeItem{" +
                        "period = '" + getPeriod() + '\'' +
                        ",v = '" + getV() + '\'' +
                        "}";
    }
}