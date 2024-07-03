package stock.price.analytics.model.annual;

//@Entity
public class LongtermDebtTotalEquityItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "LongtermDebtTotalEquityItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}