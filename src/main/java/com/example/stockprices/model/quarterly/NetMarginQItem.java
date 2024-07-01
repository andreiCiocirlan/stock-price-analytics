package com.example.stockprices.model.quarterly;

import com.example.stockprices.model.annual.PeriodWithValue;
import com.fasterxml.jackson.annotation.JsonIgnoreType;


@JsonIgnoreType
public class NetMarginQItem extends PeriodWithValue {
    @Override
    public String toString() {
        return
                "NetMarginQItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}