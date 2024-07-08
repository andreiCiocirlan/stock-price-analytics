package stock.price.analytics.model.quarterly;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import stock.price.analytics.model.annual.PeriodWithValue;


@JsonIgnoreType
public class SgaToSaleQItem extends PeriodWithValue {
    @Override
    public String toString() {
        return
                STR."SgaToSaleQItem{period = '\{period}\{'\''},v = '\{V}\{'\''}}";
    }
}