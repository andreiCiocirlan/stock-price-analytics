package stock.price.analytics.model.annual;

//@Entity
public class PeItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                STR."PeItem{period = '\{getPeriod()}\{'\''},v = '\{getV()}\{'\''}}";
    }
}