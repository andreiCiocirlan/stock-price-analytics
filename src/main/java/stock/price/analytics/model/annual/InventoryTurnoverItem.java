package stock.price.analytics.model.annual;

//@Entity
public class InventoryTurnoverItem extends PeriodWithValue {

    @Override
    public String toString() {
        return
                "InventoryTurnoverItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}