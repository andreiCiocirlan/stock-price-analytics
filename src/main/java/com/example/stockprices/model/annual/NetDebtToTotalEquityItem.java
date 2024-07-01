package com.example.stockprices.model.annual;

//@Entity
public class NetDebtToTotalEquityItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "NetDebtToTotalEquityItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}