package stock.price.analytics.model.annual;

//@Entity
public class TangibleBookValueItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "TangibleBookValueItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}