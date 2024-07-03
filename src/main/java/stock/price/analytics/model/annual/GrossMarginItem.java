package stock.price.analytics.model.annual;

//@Entity
public class GrossMarginItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "GrossMarginItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}