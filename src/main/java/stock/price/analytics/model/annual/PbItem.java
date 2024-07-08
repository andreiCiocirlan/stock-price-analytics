package stock.price.analytics.model.annual;

//@Entity
public class PbItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                STR."PbItem{period = '\{getPeriod()}\{'\''},v = '\{getV()}\{'\''}}";
    }
}