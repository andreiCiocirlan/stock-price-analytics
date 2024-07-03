package stock.price.analytics.model.annual;

//@Entity
public class PayoutRatioItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "PayoutRatioItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}