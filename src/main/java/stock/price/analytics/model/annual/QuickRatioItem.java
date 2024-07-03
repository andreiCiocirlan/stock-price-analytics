package stock.price.analytics.model.annual;

//@Entity
public class QuickRatioItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "QuickRatioItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}