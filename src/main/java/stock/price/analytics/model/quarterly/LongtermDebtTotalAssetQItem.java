package stock.price.analytics.model.quarterly;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import stock.price.analytics.model.annual.PeriodWithValue;


@JsonIgnoreType
public class LongtermDebtTotalAssetQItem extends PeriodWithValue {
    @Override
    public String toString() {
        return
                STR."LongtermDebtTotalAssetQItem{period = '\{period}\{'\''},v = '\{V}\{'\''}}";
    }
}