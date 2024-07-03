package stock.price.analytics.model.annual;

//@Entity
public class RoeItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "RoeItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}