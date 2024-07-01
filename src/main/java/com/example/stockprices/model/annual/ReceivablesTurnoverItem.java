package com.example.stockprices.model.annual;

//@Entity
public class ReceivablesTurnoverItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "ReceivablesTurnoverItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}