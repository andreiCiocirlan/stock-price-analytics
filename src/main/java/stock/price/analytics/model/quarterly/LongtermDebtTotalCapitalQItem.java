package stock.price.analytics.model.quarterly;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import stock.price.analytics.model.annual.PeriodWithValue;


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