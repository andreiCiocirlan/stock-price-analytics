package com.example.stockprices.model.quarterly;

import com.example.stockprices.model.annual.PeriodWithValue;
import com.fasterxml.jackson.annotation.JsonIgnoreType;


@JsonIgnoreType
public class LongtermDebtTotalEquityQItem extends PeriodWithValue {
    @Override
    public String toString() {
        return
                "LongtermDebtTotalEquityQItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}