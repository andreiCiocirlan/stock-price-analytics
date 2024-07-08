package stock.price.analytics.model.quarterly;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import stock.price.analytics.model.annual.PeriodWithValue;


@JsonIgnoreType
public class RotcTTMQItem extends PeriodWithValue {
    @Override
    public String toString() {
        return
                STR."RotcTTMQItem{period = '\{period}\{'\''},v = '\{V}\{'\''}}";
    }
}