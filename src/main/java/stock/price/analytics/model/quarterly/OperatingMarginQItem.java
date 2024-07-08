package stock.price.analytics.model.quarterly;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import stock.price.analytics.model.annual.PeriodWithValue;


@JsonIgnoreType
public class OperatingMarginQItem extends PeriodWithValue {
    @Override
    public String toString() {
        return
                STR."OperatingMarginQItem{period = '\{period}\{'\''},v = '\{V}\{'\''}}";
    }
}