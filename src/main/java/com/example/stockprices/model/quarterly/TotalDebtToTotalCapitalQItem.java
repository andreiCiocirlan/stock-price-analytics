package com.example.stockprices.model.quarterly;

import com.example.stockprices.model.annual.PeriodWithValue;
import com.fasterxml.jackson.annotation.JsonIgnoreType;


@JsonIgnoreType
public class TotalDebtToTotalCapitalQItem extends PeriodWithValue {
    @Override
    public String toString() {
        return
                "TotalDebtToTotalCapitalQItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}