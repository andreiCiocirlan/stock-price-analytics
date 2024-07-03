package stock.price.analytics.model.annual;

//@Entity
public class SgaToSaleItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "SgaToSaleItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}