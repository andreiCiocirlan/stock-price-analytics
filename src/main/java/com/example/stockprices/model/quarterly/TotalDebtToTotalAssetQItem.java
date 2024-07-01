package com.example.stockprices.model.quarterly;

import com.example.stockprices.model.annual.PeriodWithValue;
import com.fasterxml.jackson.annotation.JsonIgnoreType;


@JsonIgnoreType
public class TotalDebtToTotalAssetQItem extends PeriodWithValue {
    @Override
    public String toString() {
        return
                "TotalDebtToTotalAssetQItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}