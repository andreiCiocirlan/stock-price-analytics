package stock.price.analytics.model.annual;

//@Entity
public class RotcItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "RotcItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}