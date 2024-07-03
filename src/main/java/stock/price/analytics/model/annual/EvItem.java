package stock.price.analytics.model.annual;

//@Entity
public class EvItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "EvItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}