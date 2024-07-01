package com.example.stockprices.model.annual;

//@Entity
public class NetDebtToTotalCapitalItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "NetDebtToTotalCapitalItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}