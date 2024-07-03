package stock.price.analytics.model.annual;

//@Entity
public class BookValueItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "BookValueItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}