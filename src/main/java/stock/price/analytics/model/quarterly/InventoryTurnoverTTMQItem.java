package stock.price.analytics.model.quarterly;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import stock.price.analytics.model.annual.PeriodWithValue;


@JsonIgnoreType
public class InventoryTurnoverTTMQItem extends PeriodWithValue {
    @Override
    public String toString() {
        return
                STR."InventoryTurnoverTTMQItem{period = '\{period}\{'\''},v = '\{V}\{'\''}}";
    }
}