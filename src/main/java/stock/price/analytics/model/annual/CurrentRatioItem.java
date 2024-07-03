package stock.price.analytics.model.annual;

//@Entity
public class CurrentRatioItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "CurrentRatioItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}