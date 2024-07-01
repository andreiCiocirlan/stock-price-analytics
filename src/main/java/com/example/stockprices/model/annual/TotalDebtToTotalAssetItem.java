package com.example.stockprices.model.annual;

//@Entity
public class TotalDebtToTotalAssetItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "TotalDebtToTotalAssetItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}