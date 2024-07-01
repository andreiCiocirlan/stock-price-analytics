package com.example.stockprices.model.annual;

//@Entity
public class PbItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "PbItem{" +
                        "period = '" + getPeriod() + '\'' +
                        ",v = '" + getV() + '\'' +
                        "}";
    }
}