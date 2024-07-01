package com.example.stockprices.model.quarterly;

import com.example.stockprices.model.annual.PeriodWithValue;
import com.fasterxml.jackson.annotation.JsonIgnoreType;


@JsonIgnoreType
public class LongtermDebtTotalCapitalQItem extends PeriodWithValue {
    @Override
    public String toString() {
        return
                "LongtermDebtTotalCapitalQItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}