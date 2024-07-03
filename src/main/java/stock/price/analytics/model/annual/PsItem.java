package stock.price.analytics.model.annual;

//@Entity
public class PsItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "PsItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}