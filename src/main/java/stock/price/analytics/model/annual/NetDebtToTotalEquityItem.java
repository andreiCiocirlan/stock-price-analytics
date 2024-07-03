package stock.price.analytics.model.annual;

//@Entity
public class NetDebtToTotalEquityItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "NetDebtToTotalEquityItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}