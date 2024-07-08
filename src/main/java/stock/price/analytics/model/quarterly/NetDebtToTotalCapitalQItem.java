package stock.price.analytics.model.quarterly;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import stock.price.analytics.model.annual.PeriodWithValue;


@JsonIgnoreType
public class NetDebtToTotalCapitalQItem extends PeriodWithValue {
    @Override
    public String toString() {
        return
                STR."NetDebtToTotalCapitalQItem{period = '\{period}\{'\''},v = '\{V}\{'\''}}";
    }
}