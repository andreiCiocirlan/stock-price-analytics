package com.example.stockprices.model.quarterly;

import com.example.stockprices.model.annual.PeriodWithValue;
import com.fasterxml.jackson.annotation.JsonIgnoreType;


@JsonIgnoreType
public class NetDebtToTotalCapitalQItem extends PeriodWithValue {
    @Override
    public String toString() {
        return
                "NetDebtToTotalCapitalQItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}