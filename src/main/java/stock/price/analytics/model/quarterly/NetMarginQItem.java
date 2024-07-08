package stock.price.analytics.model.quarterly;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import stock.price.analytics.model.annual.PeriodWithValue;


@JsonIgnoreType
public class NetMarginQItem extends PeriodWithValue {
    @Override
    public String toString() {
        return
                STR."NetMarginQItem{period = '\{period}\{'\''},v = '\{V}\{'\''}}";
    }
}