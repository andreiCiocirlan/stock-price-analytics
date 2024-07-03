package stock.price.analytics.model.quarterly;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import stock.price.analytics.model.annual.PeriodWithValue;


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