package stock.price.analytics.model.annual;

//@Entity
public class TotalDebtToEquityItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "TotalDebtToEquityItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}