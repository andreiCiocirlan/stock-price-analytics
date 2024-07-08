package stock.price.analytics.model.quarterly;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import stock.price.analytics.model.annual.PeriodWithValue;


@JsonIgnoreType
public class PbQItem extends PeriodWithValue {
    @Override
    public String toString() {
        return
                STR."PbQItem{period = '\{period}\{'\''},v = '\{V}\{'\''}}";
    }
}