package com.example.stockprices.model.quarterly;

import com.example.stockprices.model.annual.PeriodWithValue;
import com.fasterxml.jackson.annotation.JsonIgnoreType;


@JsonIgnoreType
public class LongtermDebtTotalAssetQItem extends PeriodWithValue {
    @Override
    public String toString() {
        return
                "LongtermDebtTotalAssetQItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}