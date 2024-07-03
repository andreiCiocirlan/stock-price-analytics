package stock.price.analytics.model.annual;

//@Entity
public class CashRatioItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "CashRatioItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}