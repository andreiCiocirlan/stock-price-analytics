package stock.price.analytics.model.prices.enums;

import stock.price.analytics.model.prices.StockPriceMilestone;
import stock.price.analytics.model.prices.context.StockHighLowForPeriodContext;

public enum NewHighLowMilestone implements StockPriceMilestone<StockHighLowForPeriodContext> {

    NEW_52W_HIGH,
    NEW_4W_HIGH,
    NEW_ALL_TIME_HIGH,
    NEW_52W_LOW,
    NEW_4W_LOW,
    NEW_ALL_TIME_LOW;

    @Override
    public boolean isMetFor(StockHighLowForPeriodContext context) {
        return switch (this) {
            case NEW_52W_HIGH, NEW_ALL_TIME_HIGH, NEW_4W_HIGH ->
                    context.stock().getWeeklyHigh() > context.highLowForPeriod().getHigh();
            case NEW_52W_LOW, NEW_4W_LOW, NEW_ALL_TIME_LOW ->
                    context.stock().getWeeklyLow() < context.highLowForPeriod().getLow();
        };
    }
}
