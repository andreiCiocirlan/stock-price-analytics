package stock.price.analytics.cache.model;

import stock.price.analytics.model.prices.ohlc.MonthlyPrice;

public record MonthlyPriceWithPrevClose(MonthlyPrice monthlyPrice, double previousClose) implements PriceWithPrevClose {
}
