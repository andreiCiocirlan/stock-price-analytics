package stock.price.analytics.model.annual;

//@Entity
public class LongtermDebtTotalAssetItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "LongtermDebtTotalAssetItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}