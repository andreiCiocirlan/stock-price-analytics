package stock.price.analytics.cache.model;

import stock.price.analytics.model.prices.ohlc.AbstractPrice;
import stock.price.analytics.model.prices.ohlc.WeeklyPrice;

public record WeeklyPriceWithPrevClose(WeeklyPrice weeklyPrice, double previousClose) implements PriceWithPrevClose {
    @Override
    public AbstractPrice getPrice() {
        return weeklyPrice();
    }
}
