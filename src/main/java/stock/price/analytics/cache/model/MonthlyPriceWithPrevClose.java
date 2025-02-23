package stock.price.analytics.cache.model;

import stock.price.analytics.model.prices.ohlc.AbstractPrice;
import stock.price.analytics.model.prices.ohlc.MonthlyPrice;

public record MonthlyPriceWithPrevClose(MonthlyPrice monthlyPrice, double previousClose) implements PriceWithPrevClose {
    @Override
    public AbstractPrice getPrice() {
        return monthlyPrice();
    }
}
