package stock.price.analytics.model.quarterly;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import stock.price.analytics.model.annual.PeriodWithValue;


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